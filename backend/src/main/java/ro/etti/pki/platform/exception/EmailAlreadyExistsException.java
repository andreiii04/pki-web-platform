package ro.etti.pki.platform.exception;

/**
 * Thrown when a registration request uses an email that is already taken.
 * Mapped to HTTP 409 Conflict in {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}
