package ro.etti.pki.platform.dto.response;

import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.CertificateStatus;

import java.time.Instant;

/**
 * Public representation of a certificate, without the encrypted private key
 * (which stays in the DB and is only accessible to the internal signing service).
 *
 * @param id                 internal DB id
 * @param serialNumber       unique serial number (hex)
 * @param subjectDn          subject DN (CN, O, C)
 * @param issuerDn           DN of the issuing authority
 * @param status             effective status: {@link CertificateStatus#ACTIVE}, {@code EXPIRED}, {@code REVOKED}
 * @param issuedAt           issuance time
 * @param expiresAt          expiry time
 * @param revokedAt          set if status={@code REVOKED}; otherwise {@code null}
 * @param revocationReason   revocation reason; {@code null} if status != {@code REVOKED}
 * @param publicKeyPem       public key in PEM format (safe to expose)
 * @param certificatePem     full X.509 certificate in PEM format
 */
public record CertificateResponse(
        Long id,
        String serialNumber,
        String subjectDn,
        String issuerDn,
        CertificateStatus status,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String revocationReason,
        String publicKeyPem,
        String certificatePem
) {

    /**
     * Builds a DTO from the persisted entity.
     */
    public static CertificateResponse from(Certificate cert) {
        return new CertificateResponse(
                cert.getId(),
                cert.getSerialNumber(),
                cert.getSubjectDn(),
                cert.getIssuerDn(),
                cert.getEffectiveStatus(),
                cert.getIssuedAt(),
                cert.getExpiresAt(),
                cert.getRevokedAt(),
                cert.getRevocationReason(),
                cert.getPublicKeyPem(),
                cert.getCertificatePem());
    }
}
