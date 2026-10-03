package ro.etti.pki.platform.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateTest {

    private static final User USER = new User("jane", "jane@example.com", "hash", UserRole.USER);

    @Test
    void activeCertificateWithinValidityIsReportedActive() {
        Certificate cert = certificateExpiringAt(Instant.now().plus(1, ChronoUnit.DAYS));

        assertThat(cert.getEffectiveStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    void activeCertificatePastExpiryIsReportedExpired() {
        Certificate cert = certificateExpiringAt(Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(cert.getStatus()).isEqualTo(CertificateStatus.ACTIVE);
        assertThat(cert.getEffectiveStatus()).isEqualTo(CertificateStatus.EXPIRED);
    }

    @Test
    void revokedStatusTakesPrecedenceOverExpiry() {
        Certificate cert = certificateExpiringAt(Instant.now().minus(1, ChronoUnit.DAYS));
        cert.revoke("key compromise");

        assertThat(cert.getEffectiveStatus()).isEqualTo(CertificateStatus.REVOKED);
    }

    private static Certificate certificateExpiringAt(Instant expiresAt) {
        return new Certificate(USER, "01", "CN=Jane Doe", "CN=Test CA", "pem", "enc", "pem",
                expiresAt.minus(365, ChronoUnit.DAYS), expiresAt);
    }
}
