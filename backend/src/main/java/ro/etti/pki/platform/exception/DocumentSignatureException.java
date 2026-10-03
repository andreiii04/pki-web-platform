package ro.etti.pki.platform.exception;

/**
 * Thrown when a PDF operation fails: corrupt input, signing failure or
 * invalid verification. Mapped to HTTP 400 Bad Request or 500 as appropriate
 * in {@link GlobalExceptionHandler}.
 */
public class DocumentSignatureException extends RuntimeException {

    public DocumentSignatureException(String message) {
        super(message);
    }

    public DocumentSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
