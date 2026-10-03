package ro.etti.pki.platform.dto.response;

import ro.etti.pki.platform.entity.UserRole;

/**
 * Response of the authentication endpoints ({@code register}, {@code login}).
 * <p>
 * The frontend stores {@code accessToken} and sends it in the
 * {@code Authorization: Bearer ...} header on subsequent requests.
 * </p>
 *
 * @param accessToken compact JWS JWT
 * @param tokenType   HTTP authorization scheme, always {@code "Bearer"}
 * @param expiresInMs validity duration in milliseconds from issuance
 * @param username    authenticated username
 * @param email       user's email
 * @param role        role ({@link UserRole#USER} / {@link UserRole#ADMIN})
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        String email,
        UserRole role
) {

    /** Token type constant, avoids magic strings across the codebase. */
    public static final String BEARER = "Bearer";
}
