package ro.etti.pki.platform.exception;

/**
 * Thrown when a certificate looked up by id or serial number does not exist in the DB.
 * Mapped to HTTP 404 Not Found in {@link GlobalExceptionHandler}.
 */
public class CertificateNotFoundException extends RuntimeException {

    public CertificateNotFoundException(String message) {
        super(message);
    }
}
