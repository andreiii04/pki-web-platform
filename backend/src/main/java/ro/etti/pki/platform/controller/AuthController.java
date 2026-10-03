package ro.etti.pki.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.etti.pki.platform.dto.request.LoginRequest;
import ro.etti.pki.platform.dto.request.RegisterRequest;
import ro.etti.pki.platform.dto.response.AuthResponse;
import ro.etti.pki.platform.service.AuthService;

/**
 * Public authentication endpoints.
 *
 * <ul>
 *   <li>{@code POST /api/auth/register} — creates a new account (role {@code USER})
 *       and returns a JWT (auto-login). HTTP 201 Created on success.</li>
 *   <li>{@code POST /api/auth/login} — authenticates an existing account and returns
 *       a JWT. HTTP 200 OK on success, 401 on invalid credentials.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration and JWT login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new account",
            description = "Creates a USER account and returns a JWT (auto-login).")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Log in", description = "Returns a JWT for valid credentials.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
