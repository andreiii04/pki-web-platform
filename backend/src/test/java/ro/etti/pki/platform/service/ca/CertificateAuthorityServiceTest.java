package ro.etti.pki.platform.service.ca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.etti.pki.platform.service.crypto.TestCryptoServices;
import ro.etti.pki.platform.support.TestPki;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CertificateAuthorityServiceTest {

    @TempDir
    Path tempDir;

    private CertificateAuthorityService caService;
    private KeyPair subjectKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        Path keystore = TestPki.createCaKeystore(tempDir.resolve("ca.p12"), TestPki.CA_SUBJECT);
        var properties = TestPki.properties(keystore);

        caService = new CertificateAuthorityService(properties);
        caService.loadCertificateAuthority();

        subjectKeyPair = TestCryptoServices.initialized(properties).generateRsaKeyPair();
    }

    @Test
    void issuedCertificateIsSignedByCaAndChainsToIt() {
        X509Certificate cert = caService.issueCertificate(
                subjectKeyPair.getPublic(), "Jane Doe", "Example Org", 365);

        X509Certificate caCert = caService.getCaCertificate();
        assertThat(cert.getIssuerX500Principal()).isEqualTo(caCert.getSubjectX500Principal());
        assertThatCode(() -> cert.verify(caCert.getPublicKey())).doesNotThrowAnyException();
        assertThat(cert.getPublicKey()).isEqualTo(subjectKeyPair.getPublic());
    }

    @Test
    void issuedCertificateHasExpectedSubjectAndValidity() {
        X509Certificate cert = caService.issueCertificate(
                subjectKeyPair.getPublic(), "Jane Doe", "Example Org", 30);

        assertThat(cert.getSubjectX500Principal().getName())
                .contains("CN=Jane Doe", "O=Example Org", "C=RO");
        Duration validity = Duration.between(cert.getNotBefore().toInstant(), cert.getNotAfter().toInstant());
        assertThat(validity).isEqualTo(Duration.ofDays(30));
        assertThat(cert.getSigAlgName()).isEqualToIgnoringCase("SHA256WITHRSA");
    }

    @Test
    void issuedCertificateIsEndEntityRestrictedToSigning() {
        X509Certificate cert = caService.issueCertificate(
                subjectKeyPair.getPublic(), "Jane Doe", "Example Org", 365);

        // -1 means "not a CA"
        assertThat(cert.getBasicConstraints()).isEqualTo(-1);
        boolean[] keyUsage = cert.getKeyUsage();
        assertThat(keyUsage[0]).as("digitalSignature").isTrue();
        assertThat(keyUsage[1]).as("nonRepudiation").isTrue();
        assertThat(keyUsage[5]).as("keyCertSign").isFalse();
        assertThat(cert.getCriticalExtensionOIDs()).contains("2.5.29.15", "2.5.29.19");
    }

    @Test
    void serialNumbersAreUnique() {
        X509Certificate first = caService.issueCertificate(subjectKeyPair.getPublic(), "A", "O", 1);
        X509Certificate second = caService.issueCertificate(subjectKeyPair.getPublic(), "A", "O", 1);

        assertThat(first.getSerialNumber()).isNotEqualTo(second.getSerialNumber());
    }

    @Test
    void pemRoundTripPreservesCertificate() {
        X509Certificate cert = caService.issueCertificate(
                subjectKeyPair.getPublic(), "Jane Doe", "Example Org", 365);

        String pem = caService.certificateToPem(cert);

        assertThat(pem).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(caService.certificateFromPem(pem)).isEqualTo(cert);
    }
}
