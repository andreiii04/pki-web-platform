package ro.etti.pki.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ro.etti.pki.platform.config.OpenApiConfig;
import ro.etti.pki.platform.dto.response.VerificationResponse;
import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.CertificateStatus;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.exception.CertificateNotFoundException;
import ro.etti.pki.platform.exception.CertificateNotUsableException;
import ro.etti.pki.platform.exception.DocumentSignatureException;
import ro.etti.pki.platform.repository.CertificateRepository;
import ro.etti.pki.platform.security.AppUserDetails;
import ro.etti.pki.platform.service.DocumentSignatureService;

import java.io.IOException;

/**
 * Endpoints for signing and verifying PDF documents.
 *
 * <ul>
 *   <li>{@code POST /api/documents/sign} — authenticated. Multipart with the PDF file
 *       and the certificate id. Returns the signed PDF as a binary download.</li>
 *   <li>{@code POST /api/documents/verify} — public. Multipart with the PDF file.
 *       Returns a JSON report of all signatures found.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "PAdES signing and verification of PDF files")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    /** Defensive limit (10 MiB); larger files are rejected before being loaded into memory. */
    private static final long MAX_PDF_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentSignatureService signatureService;
    private final CertificateRepository certificateRepository;

    public DocumentController(DocumentSignatureService signatureService,
                              CertificateRepository certificateRepository) {
        this.signatureService = signatureService;
        this.certificateRepository = certificateRepository;
    }

    // -----------------------------------------------------------
    //  Signing
    // -----------------------------------------------------------

    @Operation(summary = "Sign a PDF",
            description = "Signs the uploaded PDF (max 10 MiB) with one of the caller's ACTIVE certificates and returns the signed file.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping(value = "/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> sign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("certificateId") Long certificateId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "location", required = false) String location,
            @AuthenticationPrincipal AppUserDetails principal) throws IOException {

        validatePdf(file);
        User user = principal.getUser();

        Certificate certificate = certificateRepository
                .findByIdAndUserId(certificateId, user.getId())
                .orElseThrow(() -> new CertificateNotFoundException(
                        "Certificate not found or not owned by the current user: id=" + certificateId));

        if (certificate.getStatus() != CertificateStatus.ACTIVE) {
            throw new CertificateNotUsableException(
                    "Certificate is not usable (status=" + certificate.getStatus() + ")",
                    certificate.getStatus());
        }
        if (certificate.isExpired()) {
            throw new CertificateNotUsableException(
                    "Certificate expired at " + certificate.getExpiresAt(),
                    CertificateStatus.EXPIRED);
        }

        log.info("Signing PDF: user='{}', certId={}, file='{}', size={} B",
                user.getUsername(), certificateId, file.getOriginalFilename(), file.getSize());

        byte[] signedPdf = signatureService.sign(file.getBytes(), certificate, reason, location);

        String downloadName = buildSignedFilename(file.getOriginalFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"")
                .body(signedPdf);
    }

    // -----------------------------------------------------------
    //  Verification
    // -----------------------------------------------------------

    @Operation(summary = "Verify a PDF",
            description = "Public. Reports integrity, signer, trust and certificate status for every signature in the PDF.")
    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationResponse> verify(
            @RequestParam("file") MultipartFile file) throws IOException {

        validatePdf(file);
        log.info("Verifying PDF: file='{}', size={} B",
                file.getOriginalFilename(), file.getSize());
        return ResponseEntity.ok(signatureService.verify(file.getBytes()));
    }

    // -----------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentSignatureException("PDF file is missing or empty");
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new DocumentSignatureException(
                    "File too large (" + file.getSize() + " B); limit " + MAX_PDF_SIZE_BYTES + " B");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals(MediaType.APPLICATION_PDF_VALUE)
                && !contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            throw new DocumentSignatureException(
                    "Unexpected Content-Type: " + contentType + " (expected: application/pdf)");
        }
    }

    private String buildSignedFilename(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "signed.pdf";
        }
        String trimmed = originalName.trim();
        int dotIdx = trimmed.lastIndexOf('.');
        String base = (dotIdx > 0) ? trimmed.substring(0, dotIdx) : trimmed;
        return base + "-signed.pdf";
    }
}
