package ro.etti.pki.platform.service.crypto;

/**
 * Unchecked exception thrown when a cryptographic operation fails
 * (key generation, signing/verification, AES encryption/decryption).
 * <p>
 * Wraps checked JCA exceptions ({@code GeneralSecurityException},
 * {@code IOException}) in a runtime hierarchy so that business method
 * signatures are not cluttered with {@code throws} clauses.
 * </p>
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
