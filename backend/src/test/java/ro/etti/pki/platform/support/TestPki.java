package ro.etti.pki.platform.support;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import ro.etti.pki.platform.config.properties.CaProperties;
import ro.etti.pki.platform.config.properties.CertificateProperties;
import ro.etti.pki.platform.config.properties.CryptoProperties;
import ro.etti.pki.platform.config.properties.PkiProperties;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Test fixtures: a throwaway Root CA in a PKCS#12 keystore and matching
 * {@link PkiProperties}, so crypto services can be tested without Spring or a database.
 */
public final class TestPki {

    public static final String CA_SUBJECT = "CN=PKI Platform Test Root CA,O=PKI Platform Tests,C=RO";
    public static final String KEYSTORE_PASSWORD = "test-password";
    public static final String KEY_ALIAS = "test-ca";

    private TestPki() {
    }

    public static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Creates a self-signed CA certificate with the given subject and stores it,
     * together with its private key, in a PKCS#12 keystore at {@code keystorePath}.
     */
    public static Path createCaKeystore(Path keystorePath, String subjectDn) throws Exception {
        registerBouncyCastle();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        X500Name subject = new X500Name(subjectDn);
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                new BigInteger(64, new SecureRandom()),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        X509Certificate caCert = new JcaX509CertificateConverter()
                .getCertificate(builder.build(
                        new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null, null);
        keystore.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), KEYSTORE_PASSWORD.toCharArray(),
                new java.security.cert.Certificate[]{caCert});
        try (OutputStream out = Files.newOutputStream(keystorePath)) {
            keystore.store(out, KEYSTORE_PASSWORD.toCharArray());
        }
        return keystorePath;
    }

    /** Builds {@link PkiProperties} pointing at the given keystore, with a random AES master key. */
    public static PkiProperties properties(Path keystorePath) {
        CaProperties ca = new CaProperties();
        ca.setKeystorePath(keystorePath.toString());
        ca.setKeystorePassword(KEYSTORE_PASSWORD);
        ca.setKeyAlias(KEY_ALIAS);

        byte[] aesKey = new byte[32];
        new SecureRandom().nextBytes(aesKey);
        CryptoProperties crypto = new CryptoProperties();
        crypto.setRsaKeySize(2048);
        crypto.setSignatureAlgorithm("SHA256withRSA");
        crypto.setAesMasterKey(Base64.getEncoder().encodeToString(aesKey));

        CertificateProperties certificate = new CertificateProperties();
        certificate.setValidityDays(365);
        certificate.setOrganizationDefault("PKI Web Platform Demo");
        certificate.setCountry("RO");

        PkiProperties properties = new PkiProperties();
        properties.setCa(ca);
        properties.setCrypto(crypto);
        properties.setCertificate(certificate);
        return properties;
    }
}
