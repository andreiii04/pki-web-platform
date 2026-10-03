package ro.etti.pki.platform.entity;

/**
 * Status of an X.509 certificate issued by the private CA.
 * <p>
 * Values map 1:1 to the PostgreSQL enum {@code certificate_status}
 * defined in the Flyway migration V1__init_schema.sql.
 * </p>
 *
 * <ul>
 *   <li>{@link #ACTIVE}  — valid certificate, within its validity period</li>
 *   <li>{@link #EXPIRED} — the expiry date (notAfter) has passed</li>
 *   <li>{@link #REVOKED} — revoked before expiry (compromised or lost key, etc.)</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280 — X.509 PKI</a>
 */
public enum CertificateStatus {

    /**
     * Cryptographically valid certificate within its validity period.
     * Can be used for signing and verification.
     */
    ACTIVE,

    /**
     * The expiry date (notAfter) has passed.
     * The certificate can no longer sign, but older signatures remain verifiable
     * if they were created within the validity period.
     */
    EXPIRED,

    /**
     * Revoked before expiry (compromised private key, lost access, etc.).
     * All signatures are considered invalid from the moment of revocation.
     */
    REVOKED
}
