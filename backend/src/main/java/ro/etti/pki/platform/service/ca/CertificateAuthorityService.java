package ro.etti.pki.platform.service.ca;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.etti.pki.platform.config.properties.PkiProperties;
import ro.etti.pki.platform.service.crypto.CryptoException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Certificate Authority service: loads the Root CA key/certificate from a
 * PKCS#12 keystore at startup and issues X.509 v3 certificates for users,
 * signed with the CA key.
 * <p>
 * X.509 v3 extensions set on end-entity certificates:
 * </p>
 * <ul>
 *   <li>{@code SubjectKeyIdentifier} — hash of the subject public key (RFC 5280 §4.2.1.2)</li>
 *   <li>{@code AuthorityKeyIdentifier} — hash of the CA public key (RFC 5280 §4.2.1.1)</li>
 *   <li>{@code KeyUsage}: digitalSignature + nonRepudiation (CRITICAL)</li>
 *   <li>{@code ExtendedKeyUsage}: emailProtection</li>
 *   <li>{@code BasicConstraints}: cA=false (CRITICAL) — end-entity, cannot sign other certificates</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280 — X.509 PKI</a>
 */
@Service
public class CertificateAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(CertificateAuthorityService.class);

    /** Keystore type: PKCS#12, the interoperable standard. */
    private static final String KEYSTORE_TYPE = "PKCS12";

    /** Serial number length (bits). 128 follows the CA/B Forum recommendation. */
    private static final int SERIAL_NUMBER_LENGTH_BITS = 128;

    private final PkiProperties properties;
    private final SecureRandom secureRandom;

    private X509Certificate caCertificate;
    private PrivateKey caPrivateKey;

    public CertificateAuthorityService(PkiProperties properties) {
        this.properties = properties;
        this.secureRandom = new SecureRandom();
    }

    @PostConstruct
    void loadCertificateAuthority() {
        Path keystorePath = resolveKeystorePath(properties.getCa().getKeystorePath());
        char[] password = properties.getCa().getKeystorePassword().toCharArray();
        String alias = properties.getCa().getKeyAlias();

        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (InputStream is = Files.newInputStream(keystorePath)) {
                keystore.load(is, password);
            }

            this.caPrivateKey = (PrivateKey) keystore.getKey(alias, password);
            if (this.caPrivateKey == null) {
                throw new IllegalStateException("Alias '" + alias + "' has no private key in the keystore");
            }
            this.caCertificate = (X509Certificate) keystore.getCertificate(alias);
            if (this.caCertificate == null) {
                throw new IllegalStateException("Alias '" + alias + "' has no certificate in the keystore");
            }

            log.info("CA loaded: subject='{}', expires={}, keystore='{}'",
                    caCertificate.getSubjectX500Principal().getName(),
                    caCertificate.getNotAfter().toInstant(),
                    keystorePath.toAbsolutePath());

        } catch (GeneralSecurityException | IOException e) {
            throw new CryptoException("Failed to load CA keystore: " + keystorePath, e);
        }
    }

    /**
     * Resolves the keystore path. Accepts both absolute and relative paths.
     * If it does not exist at the configured location, tries one level up
     * (fallback for running from {@code backend/} without a working directory set).
     */
    private Path resolveKeystorePath(String configured) {
        Path direct = Paths.get(configured);
        if (Files.exists(direct)) {
            return direct;
        }
        Path parent = Paths.get("..").resolve(configured).normalize();
        if (Files.exists(parent)) {
            log.warn("CA keystore not found at {}, using fallback {} (cwd={})",
                    direct.toAbsolutePath(), parent.toAbsolutePath(),
                    Paths.get("").toAbsolutePath());
            return parent;
        }
        throw new CryptoException("CA keystore not found: " + configured
                + " (cwd=" + Paths.get("").toAbsolutePath() + ")");
    }

    // -----------------------------------------------------------
    //  X.509 v3 certificate issuance
    // -----------------------------------------------------------

    /**
     * Issues an X.509 v3 certificate for a user, signed with the CA private key.
     *
     * @param subjectPublicKey the user's public key
     * @param subjectCn        Common Name for the DN (e.g. "Jane Doe")
     * @param organization     Organization for the DN (e.g. "Example Org")
     * @param validityDays     validity in days from issuance
     * @return the issued X.509 certificate
     */
    public X509Certificate issueCertificate(PublicKey subjectPublicKey, String subjectCn,
                                            String organization, int validityDays) {
        try {
            X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                    .addRDN(BCStyle.CN, subjectCn)
                    .addRDN(BCStyle.O, organization)
                    .addRDN(BCStyle.C, properties.getCertificate().getCountry())
                    .build();

            X500Name issuer = new JcaX509CertificateHolder(caCertificate).getSubject();

            BigInteger serial = new BigInteger(SERIAL_NUMBER_LENGTH_BITS, secureRandom).abs();

            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(validityDays, ChronoUnit.DAYS));

            SubjectPublicKeyInfo subjectKeyInfo = SubjectPublicKeyInfo.getInstance(subjectPublicKey.getEncoded());

            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, subject, subjectKeyInfo);

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

            // SubjectKeyIdentifier — SHA-1 hash of the subject public key (non-critical)
            builder.addExtension(Extension.subjectKeyIdentifier, false,
                    extUtils.createSubjectKeyIdentifier(subjectPublicKey));

            // AuthorityKeyIdentifier — hash of the CA public key, links the certificate to its issuer
            builder.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(caCertificate.getPublicKey()));

            // KeyUsage — CRITICAL: only digitalSignature + nonRepudiation
            // (enough for PDF signatures, no keyEncipherment / keyAgreement)
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));

            // ExtendedKeyUsage — non-critical: emailProtection (S/MIME + PAdES)
            builder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_emailProtection));

            // BasicConstraints — CRITICAL: cA=false, end-entity, cannot sign other certificates
            builder.addExtension(Extension.basicConstraints, true,
                    new BasicConstraints(false));

            ContentSigner signer = new JcaContentSignerBuilder(
                    properties.getCrypto().getSignatureAlgorithm())
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(caPrivateKey);

            X509CertificateHolder holder = builder.build(signer);
            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(holder);

        } catch (OperatorCreationException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new CryptoException("X.509 certificate issuance failed", e);
        }
    }

    // -----------------------------------------------------------
    //  PEM helpers
    // -----------------------------------------------------------

    /** Encodes an X.509 certificate as PEM (Base64 + BEGIN/END CERTIFICATE headers). */
    public String certificateToPem(X509Certificate certificate) {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(certificate);
        } catch (IOException e) {
            throw new CryptoException("Failed to PEM-encode certificate", e);
        }
        return sw.toString();
    }

    /** Parses an X.509 certificate from a PEM string. */
    public X509Certificate certificateFromPem(String pem) {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (!(obj instanceof X509CertificateHolder holder)) {
                throw new CryptoException("PEM does not contain an X.509 certificate: " + obj);
            }
            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(holder);
        } catch (IOException | CertificateException e) {
            throw new CryptoException("Failed to parse PEM certificate", e);
        }
    }

    /** Encodes a public key as PEM (BEGIN/END PUBLIC KEY). */
    public String publicKeyToPem(PublicKey publicKey) {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(sw)) {
            writer.writeObject(publicKey);
        } catch (IOException e) {
            throw new CryptoException("Failed to PEM-encode public key", e);
        }
        return sw.toString();
    }

    /** Returns the Root CA certificate loaded at startup. */
    public X509Certificate getCaCertificate() {
        return caCertificate;
    }
}
