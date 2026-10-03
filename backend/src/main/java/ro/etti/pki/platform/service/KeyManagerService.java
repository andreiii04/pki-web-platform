package ro.etti.pki.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.etti.pki.platform.config.properties.PkiProperties;
import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.repository.CertificateRepository;
import ro.etti.pki.platform.service.ca.CertificateAuthorityService;
import ro.etti.pki.platform.service.crypto.CryptoException;
import ro.etti.pki.platform.service.crypto.CryptoService;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Orchestrates the lifecycle of user keys and certificates.
 * <p>
 * Coordinates {@link CryptoService} (RSA and AES), {@link CertificateAuthorityService}
 * (X.509 issuance + PEM) and {@link CertificateRepository} (persistence).
 * </p>
 *
 * <h2>Certificate issuance flow</h2>
 * <ol>
 *   <li>Generate an RSA key pair (2048+ bits)</li>
 *   <li>Have the CA sign a certificate for the public key</li>
 *   <li>Encrypt the private key with the AES-256-GCM master key</li>
 *   <li>PEM-encode the public key and the certificate</li>
 *   <li>Persist a {@link Certificate} entity with the serial, DNs and extracted dates</li>
 * </ol>
 */
@Service
public class KeyManagerService {

    private static final Logger log = LoggerFactory.getLogger(KeyManagerService.class);

    private final CryptoService cryptoService;
    private final CertificateAuthorityService caService;
    private final CertificateRepository certificateRepository;
    private final PkiProperties properties;

    public KeyManagerService(CryptoService cryptoService,
                             CertificateAuthorityService caService,
                             CertificateRepository certificateRepository,
                             PkiProperties properties) {
        this.cryptoService = cryptoService;
        this.caService = caService;
        this.certificateRepository = certificateRepository;
        this.properties = properties;
    }

    /**
     * Issues a new certificate for the user and persists it in the DB.
     * The private key is encrypted with AES-256-GCM before storage.
     *
     * @param user         certificate owner
     * @param commonName   CN for the DN (e.g. the person's full name)
     * @param organization Organization for the DN
     * @return the persisted {@link Certificate} entity (with its id populated)
     */
    @Transactional
    public Certificate generateAndStore(User user, String commonName, String organization) {
        log.info("Issuing certificate for user='{}' (id={}), CN='{}', O='{}'",
                user.getUsername(), user.getId(), commonName, organization);

        // 1. Generate RSA key pair
        KeyPair keyPair = cryptoService.generateRsaKeyPair();

        // 2. Request certificate from the CA
        int validityDays = properties.getCertificate().getValidityDays();
        X509Certificate x509 = caService.issueCertificate(
                keyPair.getPublic(), commonName, organization, validityDays);

        // 3. Encrypt private key with the AES-GCM master key and Base64-encode it for the DB
        byte[] privateKeyPkcs8 = keyPair.getPrivate().getEncoded();
        byte[] encryptedBlob = cryptoService.encryptAes(privateKeyPkcs8, cryptoService.getMasterKey());
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBlob);

        // 4. PEM-encode public key and certificate
        String publicKeyPem = caService.publicKeyToPem(keyPair.getPublic());
        String certificatePem = caService.certificateToPem(x509);

        // 5. Build the Certificate entity with metadata extracted from the X.509
        String serialHex = x509.getSerialNumber().toString(16);
        String subjectDn = x509.getSubjectX500Principal().getName();
        String issuerDn = x509.getIssuerX500Principal().getName();
        Instant issuedAt = x509.getNotBefore().toInstant();
        Instant expiresAt = x509.getNotAfter().toInstant();

        Certificate entity = new Certificate(user, serialHex, subjectDn, issuerDn,
                publicKeyPem, encryptedBase64, certificatePem, issuedAt, expiresAt);

        Certificate persisted = certificateRepository.save(entity);
        log.info("Certificate issued: id={}, serial={}, expires at {}",
                persisted.getId(), persisted.getSerialNumber(), persisted.getExpiresAt());
        return persisted;
    }

    /**
     * Decrypts a certificate's private key and rebuilds it as a {@link PrivateKey}.
     * Used by the PDF signing flow.
     *
     * @param certificate certificate whose private key is needed
     * @return the RSA private key rebuilt from PKCS#8
     */
    @Transactional(readOnly = true)
    public PrivateKey decryptPrivateKey(Certificate certificate) {
        byte[] encryptedBlob = Base64.getDecoder().decode(certificate.getEncryptedPrivateKey());
        byte[] pkcs8Bytes = cryptoService.decryptAes(encryptedBlob, cryptoService.getMasterKey());
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException("Failed to rebuild RSA PrivateKey from PKCS#8", e);
        }
    }
}
