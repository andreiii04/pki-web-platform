package ro.etti.pki.platform.dto.response;

import java.util.List;

/**
 * Response of the {@code POST /api/documents/verify} endpoint.
 *
 * @param signed             {@code true} if the document contains at least one PAdES signature
 * @param allSignaturesValid {@code true} only if every signature passes the integrity check
 * @param signaturesCount    total number of signatures found
 * @param signatures         details for each signature (in signature field order)
 * @param messages           additional messages (e.g. "document contains no signatures")
 */
public record VerificationResponse(
        boolean signed,
        boolean allSignaturesValid,
        int signaturesCount,
        List<SignatureInfo> signatures,
        List<String> messages
) {
}
