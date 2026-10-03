package ro.etti.pki.platform.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.etti.pki.platform.config.properties.JwtProperties;
import ro.etti.pki.platform.security.AppUserDetailsService;

import java.io.IOException;

/**
 * Spring Security filter that intercepts every HTTP request, extracts the
 * JWT from the {@code Authorization: Bearer <token>} header, validates its
 * signature/expiry and populates the {@link SecurityContextHolder} with a
 * pre-authenticated {@link UsernamePasswordAuthenticationToken}.
 * <p>
 * If the token is missing or invalid, the filter does not raise an error:
 * it lets the chain continue, and the authorization rules in
 * {@code SecurityConfig} reject requests to protected endpoints
 * (via {@code JwtAuthenticationEntryPoint}).
 * </p>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 — Bearer Token Usage</a>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final JwtProperties properties;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   AppUserDetailsService userDetailsService,
                                   JwtProperties properties) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            // Already authenticated (e.g. by another filter), do not overwrite
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            log.debug("JWT rejected ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw token from the configured HTTP header. Returns {@code null}
     * if the header is missing, does not start with the expected prefix, or holds only the prefix.
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(properties.getHeader());
        String prefix = properties.getPrefix();
        if (header == null || !header.startsWith(prefix)) {
            return null;
        }
        String token = header.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
