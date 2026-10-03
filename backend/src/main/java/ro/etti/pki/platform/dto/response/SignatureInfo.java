package ro.etti.pki.platform.dto.response;

import ro.etti.pki.platform.entity.CertificateStatus;

import java.time.Instant;

/**
 * Details of a single PAdES signature extracted from a PDF
 * by {@code DocumentSignatureService.verify(...)}.
 *
 * @param fieldName           name of the PDF signature field (e.g. "sig1")
 * @param signerCommonName    signer's CN (extracted from the certificate subject DN)
 * @param signerSubjectDn     signer's full DN
 * @param issuerDn            DN of the issuing authority (the CA)
 * @param serialNumber        serial number of the signer's certificate (hex)
 * @param signedAt            signing time (from the PKCS#7 SigningTime attribute)
 * @param integrityValid      {@code true} if the cryptographic signature validates
 *                            (document hash = signed hash)
 * @param trusted             {@code true} if the certificate was signed by this platform's CA key
 * @param certificateStatus   effective certificate status ({@code ACTIVE},
 *                            {@code REVOKED}, {@code EXPIRED}); {@code null} if the
 *                            certificate is not trusted or not in the DB
 */
public record SignatureInfo(
        String fieldName,
        String signerCommonName,
        String signerSubjectDn,
        String issuerDn,
        String serialNumber,
        Instant signedAt,
        boolean integrityValid,
        boolean trusted,
        CertificateStatus certificateStatus
) {
}
