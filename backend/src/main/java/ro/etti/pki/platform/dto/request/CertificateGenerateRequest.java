package ro.etti.pki.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/certificates/generate}.
 *
 * @param commonName   Common Name (CN): full name of the certificate holder
 *                     (e.g. "Jane Doe"). Appears in the Subject DN.
 * @param organization Organization (O), optional. When missing, the value of
 *                     {@code pki.certificate.organization-default} is used.
 */
public record CertificateGenerateRequest(
        @NotBlank
        @Size(min = 1, max = 100)
        String commonName,

        @Size(max = 100)
        String organization
) {
}
