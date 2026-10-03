package ro.etti.pki.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/auth/register}.
 *
 * @param username 3–50 characters, letters/digits/underscore/dot only
 * @param email    valid email address, at most 255 characters
 * @param password 8–100 characters; additional complexity checks are the
 *                 frontend's responsibility (password strength estimation)
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.]+$",
                message = "Username may only contain letters, digits, underscores and dots")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
