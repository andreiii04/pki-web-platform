package ro.etti.pki.platform.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for {@code POST /api/auth/login}.
 *
 * @param username account username (case-sensitive)
 * @param password plaintext password (checked with BCrypt against the stored hash)
 */
public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        String password
) {
}
