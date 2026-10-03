package ro.etti.pki.platform.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JSON Web Token properties: HMAC secret, expiry, issuer and transport
 * conventions (HTTP header + Bearer prefix).
 * <p>
 * Binds the {@code jwt:} section of {@code application.yml}. Sensitive values
 * ({@code secret}, {@code expirationMs}) come from environment variables defined
 * in {@code .env} (gitignored).
 * </p>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>The key must be at least 256 bits (32 bytes) after Base64 decoding
 *       for HS256; 512 bits (64 bytes) is recommended for HS512.</li>
 *   <li>The decoded key length is checked in
 *       {@link ro.etti.pki.platform.security.jwt.JwtService} at startup; here
 *       only the Base64 format is validated.</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 — JSON Web Token</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7515">RFC 7515 — JSON Web Signature</a>
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /**
     * Base64-encoded HMAC secret used to sign JWTs.
     * Generate it with {@code openssl rand -base64 64} (64 random bytes
     * = 512 bits, enough for HS512).
     */
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9+/]+=*$")
    private String secret;

    /**
     * Token lifetime in milliseconds.
     * Minimum 1 second; typical values: 3,600,000 (1h), 86,400,000 (24h).
     */
    @Min(1000)
    private long expirationMs;

    /**
     * The {@code iss} (issuer) claim included in the token payload.
     * Checked during validation to reject tokens issued by another party.
     */
    @NotBlank
    private String issuer;

    /**
     * Name of the HTTP header sent by the client (usually
     * {@code Authorization}).
     */
    @NotBlank
    private String header;

    /**
     * Value prefix in the header (usually {@code "Bearer "}, with a trailing space).
     */
    @NotBlank
    private String prefix;

    public JwtProperties() {
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
