package ro.etti.pki.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.etti.pki.platform.config.properties.JwtProperties;
import ro.etti.pki.platform.dto.request.LoginRequest;
import ro.etti.pki.platform.dto.request.RegisterRequest;
import ro.etti.pki.platform.dto.response.AuthResponse;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.entity.UserRole;
import ro.etti.pki.platform.exception.EmailAlreadyExistsException;
import ro.etti.pki.platform.exception.UsernameAlreadyExistsException;
import ro.etti.pki.platform.repository.UserRepository;
import ro.etti.pki.platform.security.AppUserDetails;
import ro.etti.pki.platform.security.jwt.JwtService;

/**
 * Orchestrates the registration and login flows.
 * <p>
 * Used by {@code AuthController}. Covers:
 * </p>
 * <ul>
 *   <li>{@link #register(RegisterRequest)} — uniqueness checks, BCrypt hashing,
 *       persistence and immediate JWT issuance (auto-login after registration).</li>
 *   <li>{@link #login(LoginRequest)} — delegates to the {@link AuthenticationManager}
 *       ({@code DaoAuthenticationProvider} → {@code UserDetailsService}
 *       → BCrypt match) and issues a JWT on success.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Registers a new user with role {@link UserRole#USER} and immediately
     * returns a JWT (auto-login).
     *
     * @throws UsernameAlreadyExistsException if the username is already taken
     * @throws EmailAlreadyExistsException    if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.username(), request.email(), passwordHash, UserRole.USER);
        user = userRepository.save(user);

        log.info("Registered new user: id={}, username='{}'",
                user.getId(), user.getUsername());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user and issues a JWT on success.
     * Throws {@code AuthenticationException} (usually {@code BadCredentialsException})
     * if the username does not exist or the password does not match; it is caught by
     * {@code GlobalExceptionHandler} and mapped to HTTP 401.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        log.info("Successful login: username='{}', role={}",
                user.getUsername(), user.getRole());

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                AuthResponse.BEARER,
                jwtProperties.getExpirationMs(),
                user.getUsername(),
                user.getEmail(),
                user.getRole());
    }
}
