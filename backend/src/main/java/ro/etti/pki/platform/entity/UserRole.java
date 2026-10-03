package ro.etti.pki.platform.entity;

/**
 * Role of a user in the PKI platform.
 * <p>
 * Values map 1:1 to the PostgreSQL enum {@code user_role}
 * defined in the Flyway migration V1__init_schema.sql.
 * </p>
 *
 * <ul>
 *   <li>{@link #USER}  — standard user, can issue and apply their own signatures</li>
 *   <li>{@link #ADMIN} — administrator, intended for certificate revocation and platform management</li>
 * </ul>
 */
public enum UserRole {

    /**
     * Standard user. Permissions:
     * <ul>
     *   <li>Issue their own certificates</li>
     *   <li>Sign PDFs with their own certificates</li>
     * </ul>
     */
    USER,

    /**
     * Administrator. Reserved for future admin features (not yet exposed by the API):
     * <ul>
     *   <li>Revoking certificates of any user</li>
     *   <li>Viewing all certificates on the platform</li>
     *   <li>User management</li>
     * </ul>
     */
    ADMIN
}
