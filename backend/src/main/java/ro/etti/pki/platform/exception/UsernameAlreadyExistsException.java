package ro.etti.pki.platform.exception;

/**
 * Thrown when a registration request uses a username that is already taken.
 * Mapped to HTTP 409 Conflict in {@link GlobalExceptionHandler}.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already in use: " + username);
    }
}
