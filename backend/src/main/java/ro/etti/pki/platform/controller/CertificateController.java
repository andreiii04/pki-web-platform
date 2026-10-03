package ro.etti.pki.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.etti.pki.platform.config.OpenApiConfig;
import ro.etti.pki.platform.config.properties.PkiProperties;
import ro.etti.pki.platform.dto.request.CertificateGenerateRequest;
import ro.etti.pki.platform.dto.response.CertificateResponse;
import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.repository.CertificateRepository;
import ro.etti.pki.platform.security.AppUserDetails;
import ro.etti.pki.platform.service.KeyManagerService;

import java.util.List;

/**
 * Endpoints for managing the authenticated user's certificates.
 *
 * <ul>
 *   <li>{@code POST /api/certificates/generate} — issues an X.509 certificate
 *       for the current user, using the {@code CN} from the request and either
 *       the {@code O} from the request or the configured default.</li>
 *   <li>{@code GET /api/certificates/my} — lists the current user's
 *       certificates (all statuses).</li>
 * </ul>
 *
 * Both require a valid JWT.
 */
@RestController
@RequestMapping("/api/certificates")
@Tag(name = "Certificates", description = "X.509 certificates of the authenticated user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CertificateController {

    private static final Logger log = LoggerFactory.getLogger(CertificateController.class);

    private final KeyManagerService keyManagerService;
    private final CertificateRepository certificateRepository;
    private final PkiProperties pkiProperties;

    public CertificateController(KeyManagerService keyManagerService,
                                 CertificateRepository certificateRepository,
                                 PkiProperties pkiProperties) {
        this.keyManagerService = keyManagerService;
        this.certificateRepository = certificateRepository;
        this.pkiProperties = pkiProperties;
    }

    @Operation(summary = "Issue a certificate",
            description = "Generates an RSA key pair and issues an X.509 v3 certificate signed by the platform CA.")
    @PostMapping("/generate")
    public ResponseEntity<CertificateResponse> generate(
            @Valid @RequestBody CertificateGenerateRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        User user = principal.getUser();
        String organization = (request.organization() == null || request.organization().isBlank())
                ? pkiProperties.getCertificate().getOrganizationDefault()
                : request.organization();

        log.info("Certificate issuance requested: user='{}', CN='{}', O='{}'",
                user.getUsername(), request.commonName(), organization);

        Certificate cert = keyManagerService.generateAndStore(
                user, request.commonName(), organization);

        return ResponseEntity.status(HttpStatus.CREATED).body(CertificateResponse.from(cert));
    }

    @Operation(summary = "List my certificates")
    @GetMapping("/my")
    public ResponseEntity<List<CertificateResponse>> listMyCertificates(
            @AuthenticationPrincipal AppUserDetails principal) {

        Long userId = principal.getUser().getId();
        List<CertificateResponse> certificates = certificateRepository.findByUserId(userId).stream()
                .map(CertificateResponse::from)
                .toList();

        return ResponseEntity.ok(certificates);
    }
}
