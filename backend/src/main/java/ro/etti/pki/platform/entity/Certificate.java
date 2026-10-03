package ro.etti.pki.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * X.509 digital certificate issued by the private CA to a user.
 * <p>
 * Maps the PostgreSQL {@code certificates} table. Stores both the full
 * certificate in PEM format and its individual components
 * (public key, encrypted private key) for quick access.
 * </p>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5280">RFC 5280 — X.509 PKI</a>
 */
@Entity
@Table(name = "certificates")
@EntityListeners(AuditingEntityListener.class)
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner of the certificate.
     * LAZY ManyToOne relation: the user is only loaded when needed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * System-wide unique serial number (cryptographically random, generated at issuance).
     * Stored as a hex string (max 64 characters = 256 bits).
     */
    @Column(name = "serial_number", nullable = false, unique = true, length = 64)
    private String serialNumber;

    /**
     * Subject Distinguished Name (CN, O, C in X.500 format).
     * Example: {@code CN=Jane Doe, O=Example Org, C=RO}
     */
    @Column(name = "subject_dn", nullable = false, length = 512)
    private String subjectDn;

    /**
     * Issuer Distinguished Name (the platform's internal CA).
     */
    @Column(name = "issuer_dn", nullable = false, length = 512)
    private String issuerDn;

    /**
     * Public key in PEM format (Base64 + BEGIN/END PUBLIC KEY headers).
     */
    @Column(name = "public_key_pem", nullable = false, columnDefinition = "TEXT")
    private String publicKeyPem;

    /**
     * Private key encrypted with AES-256-GCM (master key from the environment).
     * Stored as Base64 after encryption.
     */
    @Column(name = "encrypted_private_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    /**
     * Full X.509 certificate in PEM format (public key + metadata + CA signature).
     */
    @Column(name = "certificate_pem", nullable = false, columnDefinition = "TEXT")
    private String certificatePem;

    /**
     * Status, mapped to the native PostgreSQL enum {@code certificate_status}.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "certificate_status")
    private CertificateStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Revocation time. NULL if status != REVOKED.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Revocation reason (optional). NULL if status != REVOKED.
     */
    @Column(name = "revocation_reason", length = 255)
    private String revocationReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // -----------------------------------------------------------
    //  Constructors
    // -----------------------------------------------------------

    /**
     * Protected no-arg constructor required by JPA.
     */
    protected Certificate() {
    }

    /**
     * Creates a newly issued certificate.
     */
    public Certificate(User user, String serialNumber, String subjectDn, String issuerDn,
                       String publicKeyPem, String encryptedPrivateKey, String certificatePem,
                       Instant issuedAt, Instant expiresAt) {
        this.user = user;
        this.serialNumber = serialNumber;
        this.subjectDn = subjectDn;
        this.issuerDn = issuerDn;
        this.publicKeyPem = publicKeyPem;
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.certificatePem = certificatePem;
        this.status = CertificateStatus.ACTIVE;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    // -----------------------------------------------------------
    //  Domain methods (simple business logic on the entity)
    // -----------------------------------------------------------

    /**
     * Marks the certificate as revoked.
     * Sets status = REVOKED and populates revokedAt with the current time.
     *
     * @param reason revocation reason (compromised key, loss, etc.)
     */
    public void revoke(String reason) {
        this.status = CertificateStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    /**
     * Checks whether the certificate has expired (current time > expiresAt).
     * Does not change the status in the DB.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Status as reported to clients: an {@code ACTIVE} certificate whose
     * expiry date has passed is reported as {@code EXPIRED}. The stored
     * status is not modified.
     */
    public CertificateStatus getEffectiveStatus() {
        return (status == CertificateStatus.ACTIVE && isExpired())
                ? CertificateStatus.EXPIRED
                : status;
    }

    // -----------------------------------------------------------
    //  Getters & Setters
    // -----------------------------------------------------------

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public void setStatus(CertificateStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // -----------------------------------------------------------
    //  equals & hashCode
    // -----------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Certificate cert)) return false;
        return id != null && id.equals(cert.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", subjectDn='" + subjectDn + '\'' +
                ", status=" + status +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
