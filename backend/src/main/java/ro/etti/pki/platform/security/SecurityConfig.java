package ro.etti.pki.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ro.etti.pki.platform.security.jwt.JwtAuthenticationFilter;

import java.util.List;

/**
 * Main Spring Security configuration for the PKI platform.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><b>Stateless</b> — no {@code HttpSession}; every request must carry
 *       a valid JWT.</li>
 *   <li><b>CSRF disabled</b> — CSRF protection matters for cookie-based sessions.
 *       The JWT travels in the {@code Authorization} header, which browsers never
 *       attach automatically to cross-site requests.</li>
 *   <li><b>Dev CORS</b> — only {@code http://localhost:5173} (Vite's default port)
 *       is allowed. In production the list must be restricted to the real domain.</li>
 *   <li><b>BCrypt</b> for password hashing (default strength = 10).</li>
 * </ul>
 *
 * <h2>Public endpoints</h2>
 * <ul>
 *   <li>{@code POST /api/auth/**} — register, login</li>
 *   <li>{@code POST /api/documents/verify} — anyone can verify a signed PDF</li>
 *   <li>{@code /v3/api-docs/**}, {@code /swagger-ui/**}, {@code /swagger-ui.html} — API documentation</li>
 * </ul>
 *
 * <h2>Authenticated endpoints</h2>
 * All others, in particular:
 * <ul>
 *   <li>{@code POST /api/certificates/generate}</li>
 *   <li>{@code GET  /api/certificates/my}</li>
 *   <li>{@code POST /api/documents/sign}</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/6.4/index.html">Spring Security Reference</a>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/auth/**",
            "/api/documents/verify"
    };

    /**
     * Internal path Spring Boot forwards to for 4xx/5xx errors not handled
     * elsewhere. It must be permitted explicitly to avoid a misleading 401.
     */
    private static final String ERROR_ENDPOINT = "/error";

    private static final List<String> CORS_ALLOWED_ORIGINS_DEV = List.of(
            "http://localhost:5173"
    );

    private static final List<String> CORS_ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          AppUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.userDetailsService = userDetailsService;
    }

    // -----------------------------------------------------------
    //  Filter chain
    // -----------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ERROR_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // -----------------------------------------------------------
    //  Authentication
    // -----------------------------------------------------------

    /**
     * Provider for username + password authentication against the DB,
     * using BCrypt hashes.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} for the login flow
     * in {@code AuthService}.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt with default strength (10). A single bean reused for
     * registration (hashing) and login (matching).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // -----------------------------------------------------------
    //  CORS
    // -----------------------------------------------------------

    /**
     * CORS configuration for {@code /api/**} routes.
     * <p>
     * {@code allowCredentials=true} allows cookies / the {@code Authorization}
     * header to be sent cross-origin. The {@code Authorization} response header
     * is exposed explicitly so the frontend can read it.
     * </p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(CORS_ALLOWED_ORIGINS_DEV);
        config.setAllowedMethods(CORS_ALLOWED_METHODS);
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
