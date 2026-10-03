package ro.etti.pki.platform.service.crypto;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.etti.pki.platform.config.properties.PkiProperties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Base64;

/**
 * Core cryptographic primitives: RSA key generation, digital signatures
 * (SHA-2 + RSA, configurable) and AES-256-GCM symmetric encryption/decryption.
 * <p>
 * All operations explicitly use the Bouncy Castle provider ("BC"),
 * registered in {@code PkiPlatformApplication#registerBouncyCastle()}.
 * </p>
 *
 * @see <a href="https://csrc.nist.gov/publications/detail/sp/800-38d/final">NIST SP 800-38D — GCM Mode</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8017">RFC 8017 — PKCS #1 v2.2 (RSA)</a>
 */
@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    /**
     * AES mode used to encrypt private keys: authenticated encryption (AEAD).
     * GCM provides confidentiality + integrity without a separate HMAC (unlike CBC + HMAC).
     */
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";

    /** Key algorithm for {@link SecretKeySpec}. */
    private static final String AES_KEY_ALGORITHM = "AES";

    /** AES-GCM IV length (bytes). 96 bits is the NIST SP 800-38D standard. */
    private static final int GCM_IV_LENGTH_BYTES = 12;

    /** AES-GCM authentication tag length (bits). 128 is the maximum and recommended value. */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** AES master key size (bytes): AES-256. */
    private static final int AES_KEY_LENGTH_BYTES = 32;

    private final PkiProperties properties;
    private final SecureRandom secureRandom;

    /**
     * AES-256 master key derived from config in {@code @PostConstruct}.
     * Used to encrypt RSA private keys before they are stored in the DB.
     */
    private SecretKey masterKey;

    public CryptoService(PkiProperties properties) {
        this.properties = properties;
        this.secureRandom = new SecureRandom();
    }

    @PostConstruct
    void init() {
        this.masterKey = deriveAesMasterKey(properties.getCrypto().getAesMasterKey());
        log.info("CryptoService initialized: RSA={} bits, signature={}",
                properties.getCrypto().getRsaKeySize(),
                properties.getCrypto().getSignatureAlgorithm());
    }

    // -----------------------------------------------------------
    //  RSA — key pair generation
    // -----------------------------------------------------------

    /**
     * Generates an RSA key pair of the size configured in
     * {@code pki.crypto.rsa-key-size} (2048-4096 bits).
     *
     * @return key pair (public key + private key)
     */
    public KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                    BouncyCastleProvider.PROVIDER_NAME);
            generator.initialize(properties.getCrypto().getRsaKeySize(), secureRandom);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA key pair generation failed", e);
        }
    }

    // -----------------------------------------------------------
    //  Digital signatures
    // -----------------------------------------------------------

    /**
     * Signs the data with the configured algorithm (e.g. SHA256withRSA).
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(
                    properties.getCrypto().getSignatureAlgorithm(),
                    BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(privateKey, secureRandom);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Signing data failed", e);
        }
    }

    /**
     * Verifies a signature against the data and the public key.
     *
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(
                    properties.getCrypto().getSignatureAlgorithm(),
                    BouncyCastleProvider.PROVIDER_NAME);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Signature verification failed", e);
        }
    }

    // -----------------------------------------------------------
    //  AES-256-GCM — encryption / decryption
    // -----------------------------------------------------------

    /**
     * Encrypts with AES-256-GCM using a random 12-byte IV.
     * <p>
     * Output format: {@code IV (12 bytes) || CIPHERTEXT+TAG}.
     * The IV is stored unencrypted next to the ciphertext (standard practice
     * for GCM: the IV does not need to be secret, only unique per key).
     * </p>
     */
    public byte[] encryptAes(byte[] plaintext, SecretKey key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION,
                    BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new CryptoException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypts a blob produced by {@link #encryptAes(byte[], SecretKey)}.
     * Throws {@link CryptoException} if the GCM tag does not match
     * (tampered data or wrong key).
     */
    public byte[] decryptAes(byte[] cipherBlob, SecretKey key) {
        if (cipherBlob.length <= GCM_IV_LENGTH_BYTES) {
            throw new CryptoException("Encrypted blob too short: missing IV or ciphertext");
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(cipherBlob, 0, iv, 0, GCM_IV_LENGTH_BYTES);

            byte[] ciphertext = new byte[cipherBlob.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(cipherBlob, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION,
                    BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("AES-GCM decryption failed: tampered data or wrong key", e);
        }
    }

    // -----------------------------------------------------------
    //  AES master key
    // -----------------------------------------------------------

    /**
     * Decodes an AES-256 master key from a Base64 string and validates it.
     *
     * @param base64MasterKey Base64-encoded 32-byte key
     * @return {@link SecretKey} for AES-256
     * @throws CryptoException if the Base64 is invalid or the length is not 32 bytes
     */
    public SecretKey deriveAesMasterKey(String base64MasterKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64MasterKey);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("AES master key is not valid Base64", e);
        }
        if (keyBytes.length != AES_KEY_LENGTH_BYTES) {
            throw new CryptoException("AES master key is " + keyBytes.length
                    + " bytes; AES-256 requires exactly " + AES_KEY_LENGTH_BYTES);
        }
        return new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
    }

    /**
     * Returns the AES-256 master key derived from config at startup.
     * Used by {@code KeyManagerService} to encrypt private keys before
     * persisting them.
     */
    public SecretKey getMasterKey() {
        return masterKey;
    }
}
