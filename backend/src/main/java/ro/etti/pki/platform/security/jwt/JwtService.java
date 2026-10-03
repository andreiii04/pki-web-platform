package ro.etti.pki.platform.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ro.etti.pki.platform.config.properties.JwtProperties;
import ro.etti.pki.platform.entity.User;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates JWTs.
 * <p>
 * Uses the <a href="https://github.com/jwtk/jjwt">JJWT 0.13</a> library.
 * Signature algorithm: HMAC-SHA, chosen automatically from the key length
 * (HS256 / HS384 / HS512). HS512 requires a decoded key of at least
 * 64 bytes (512 bits).
 * </p>
 *
 * <h2>Issued claims</h2>
 * <ul>
 *   <li>{@code sub} — the user's username</li>
 *   <li>{@code iss} — the issuer from {@link JwtProperties}</li>
 *   <li>{@code iat} — issuance time</li>
 *   <li>{@code exp} — expiry time ({@code iat + expirationMs})</li>
 *   <li>{@code role} — the role ({@code USER} / {@code ADMIN}) for client-side authorization</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 — JSON Web Token</a>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Name of the custom claim holding the user's role. */
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;

    /**
     * HMAC key derived once in {@code @PostConstruct} from
     * {@code jwt.secret} (Base64). Reused for every issuance/validation.
     */
    private SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.getSecret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("jwt.secret is not valid Base64", e);
        }
        try {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "jwt.secret is only " + keyBytes.length + " bytes after Base64 decoding; "
                            + "HMAC-SHA requires at least 32 bytes (256 bits). Regenerate it with "
                            + "'openssl rand -base64 64'.", e);
        }
        log.info("JwtService initialized: issuer='{}', expirationMs={}, keyLength={} bytes",
                properties.getIssuer(), properties.getExpirationMs(), keyBytes.length);
    }

    // -----------------------------------------------------------
    //  Token issuance
    // -----------------------------------------------------------

    /**
     * Issues a JWT for a user persisted in the DB.
     *
     * @param user the authenticated entity
     * @return compact JWS token (header.payload.signature, Base64URL-encoded)
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claims(Map.of(CLAIM_ROLE, user.getRole().name()))
                .signWith(signingKey)
                .compact();
    }

    // -----------------------------------------------------------
    //  Parsing and validation
    // -----------------------------------------------------------

    /**
     * Extracts the username ({@code sub}) from a token.
     * Throws {@link JwtException} if the token is invalid or expired.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Checks a token against a {@link UserDetails}: the token's username
     * must match and the token must not be expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            return username != null
                    && username.equals(userDetails.getUsername())
                    && expiration != null
                    && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
