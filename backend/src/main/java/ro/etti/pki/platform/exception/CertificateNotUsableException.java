package ro.etti.pki.platform.exception;

import ro.etti.pki.platform.entity.CertificateStatus;

/**
 * Thrown when a certificate exists but cannot be used for signing
 * (status REVOKED or EXPIRED, or it belongs to another user).
 * Mapped to HTTP 422 Unprocessable Entity in {@link GlobalExceptionHandler}.
 */
public class CertificateNotUsableException extends RuntimeException {

    private final CertificateStatus status;

    public CertificateNotUsableException(String message, CertificateStatus status) {
        super(message);
        this.status = status;
    }

    public CertificateNotUsableException(String message) {
        this(message, null);
    }

    public CertificateStatus getStatus() {
        return status;
    }
}
