package ro.etti.pki.platform.service.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.etti.pki.platform.support.TestPki;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    private static final byte[] PLAINTEXT = "private key material".getBytes(StandardCharsets.UTF_8);

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        TestPki.registerBouncyCastle();
        cryptoService = new CryptoService(TestPki.properties(Path.of("unused.p12")));
        cryptoService.init();
    }

    @Test
    void generatesRsaKeyPairOfConfiguredSize() {
        KeyPair keyPair = cryptoService.generateRsaKeyPair();

        assertThat(keyPair.getPublic()).isInstanceOf(RSAPublicKey.class);
        assertThat(((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void aesGcmRoundTripRestoresPlaintext() {
        SecretKey key = cryptoService.getMasterKey();

        byte[] encrypted = cryptoService.encryptAes(PLAINTEXT, key);

        assertThat(encrypted).isNotEqualTo(PLAINTEXT);
        assertThat(cryptoService.decryptAes(encrypted, key)).isEqualTo(PLAINTEXT);
    }

    @Test
    void aesGcmUsesFreshIvForEveryEncryption() {
        SecretKey key = cryptoService.getMasterKey();

        byte[] first = cryptoService.encryptAes(PLAINTEXT, key);
        byte[] second = cryptoService.encryptAes(PLAINTEXT, key);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void aesGcmDetectsTamperedCiphertext() {
        SecretKey key = cryptoService.getMasterKey();
        byte[] encrypted = cryptoService.encryptAes(PLAINTEXT, key);

        encrypted[encrypted.length - 1] ^= 0x01;

        assertThatThrownBy(() -> cryptoService.decryptAes(encrypted, key))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void aesGcmRejectsWrongKey() {
        byte[] encrypted = cryptoService.encryptAes(PLAINTEXT, cryptoService.getMasterKey());
        SecretKey otherKey = cryptoService.deriveAesMasterKey(
                Base64.getEncoder().encodeToString(new byte[32]));

        assertThatThrownBy(() -> cryptoService.decryptAes(encrypted, otherKey))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void aesGcmRejectsTruncatedBlob() {
        assertThatThrownBy(() -> cryptoService.decryptAes(new byte[12], cryptoService.getMasterKey()))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void masterKeyMustDecodeToExactly32Bytes() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> cryptoService.deriveAesMasterKey(shortKey))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("exactly 32");
    }

    @Test
    void signatureVerifiesOnlyForOriginalData() {
        KeyPair keyPair = cryptoService.generateRsaKeyPair();

        byte[] signature = cryptoService.sign(PLAINTEXT, keyPair.getPrivate());

        assertThat(cryptoService.verify(PLAINTEXT, signature, keyPair.getPublic())).isTrue();
        assertThat(cryptoService.verify("tampered".getBytes(StandardCharsets.UTF_8),
                signature, keyPair.getPublic())).isFalse();
    }
}
