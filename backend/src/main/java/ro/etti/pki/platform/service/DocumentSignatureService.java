package ro.etti.pki.platform.service;

import com.itextpdf.kernel.crypto.DigestAlgorithms;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignatureUtil;
import com.itextpdf.signatures.SignerProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.etti.pki.platform.dto.response.SignatureInfo;
import ro.etti.pki.platform.dto.response.VerificationResponse;
import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.CertificateStatus;
import ro.etti.pki.platform.exception.DocumentSignatureException;
import ro.etti.pki.platform.repository.CertificateRepository;
import ro.etti.pki.platform.service.ca.CertificateAuthorityService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF signing and verification following PAdES
 * (PDF Advanced Electronic Signatures), built on iText 9.
 *
 * <h2>Signing</h2>
 * <ul>
 *   <li>Hash: SHA-256</li>
 *   <li>Signature provider: Bouncy Castle</li>
 *   <li>CMS format: CAdES (PAdES Baseline B-B subset)</li>
 *   <li>Chain: user certificate + CA certificate (enough for manual validation)</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * For each signature in the PDF:
 * <ul>
 *   <li>Validates cryptographic integrity (document hash = signed hash)</li>
 *   <li>Marks it as "trusted" only if the signer certificate was signed by this platform's CA key</li>
 *   <li>For trusted signers, looks up the serial in the DB to return the current {@link CertificateStatus}</li>
 * </ul>
 *
 * @see <a href="https://www.etsi.org/deliver/etsi_en/319100_319199/31914201/01.01.01_60/en_31914201v010101p.pdf">ETSI EN 319 142-1 — PAdES Baseline</a>
 */
@Service
public class DocumentSignatureService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSignatureService.class);

    /** Pattern for extracting the CN from an X.500 DN (e.g. "CN=Jane Doe,O=..."). */
    private static final Pattern CN_PATTERN = Pattern.compile("CN=([^,]+)");

    private final KeyManagerService keyManagerService;
    private final CertificateAuthorityService caService;
    private final CertificateRepository certificateRepository;

    public DocumentSignatureService(KeyManagerService keyManagerService,
                                    CertificateAuthorityService caService,
                                    CertificateRepository certificateRepository) {
        this.keyManagerService = keyManagerService;
        this.caService = caService;
        this.certificateRepository = certificateRepository;
    }

    // -----------------------------------------------------------
    //  PAdES signing
    // -----------------------------------------------------------

    /**
     * Signs a PDF with the private key of the given certificate.
     *
     * @param pdfBytes    content of the unsigned PDF
     * @param certificate certificate used for signing (must be ACTIVE)
     * @param reason      stated signing reason (optional, stored in the signature dictionary)
     * @param location    stated location (optional, stored in the signature dictionary)
     * @return content of the signed PDF
     */
    public byte[] sign(byte[] pdfBytes, Certificate certificate, String reason, String location) {
        try {
            X509Certificate userCert = caService.certificateFromPem(certificate.getCertificatePem());
            X509Certificate caCert = caService.getCaCertificate();
            PrivateKey privateKey = keyManagerService.decryptPrivateKey(certificate);

            try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                PdfSigner signer = new PdfSigner(reader, out, new StampingProperties());

                SignerProperties signerProperties = new SignerProperties()
                        .setFieldName("Signature-" + Instant.now().toEpochMilli())
                        .setReason(reason != null && !reason.isBlank()
                                ? reason
                                : "Signed via PKI Web Platform")
                        .setLocation(location != null ? location : "");
                signer.setSignerProperties(signerProperties);

                java.security.cert.Certificate[] chain = { userCert, caCert };

                IExternalSignature pks = new PrivateKeySignature(
                        privateKey, DigestAlgorithms.SHA256, BouncyCastleProvider.PROVIDER_NAME);
                IExternalDigest digest = new BouncyCastleDigest();

                signer.signDetached(digest, pks, chain, null, null, null,
                        0, PdfSigner.CryptoStandard.CADES);

                byte[] signed = out.toByteArray();
                log.info("PDF signed: serial={}, input size={} B, output size={} B",
                        certificate.getSerialNumber(), pdfBytes.length, signed.length);
                return signed;
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new DocumentSignatureException("PDF signing failed", e);
        }
    }

    // -----------------------------------------------------------
    //  PAdES verification
    // -----------------------------------------------------------

    /**
     * Verifies every signature in a PDF and returns a detailed report.
     * Does not throw if a signature is invalid; returns {@code integrityValid=false} instead.
     */
    public VerificationResponse verify(byte[] pdfBytes) {
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument doc = new PdfDocument(reader)) {

            SignatureUtil util = new SignatureUtil(doc);
            List<String> signatureNames = util.getSignatureNames();

            if (signatureNames.isEmpty()) {
                return new VerificationResponse(false, false, 0,
                        List.of(), List.of("The document contains no signatures"));
            }

            X509Certificate caCert = caService.getCaCertificate();
            List<SignatureInfo> infos = new ArrayList<>(signatureNames.size());

            for (String name : signatureNames) {
                infos.add(buildSignatureInfo(name, util, caCert));
            }

            boolean allValid = infos.stream().allMatch(SignatureInfo::integrityValid);
            return new VerificationResponse(true, allValid, signatureNames.size(),
                    infos, List.of());

        } catch (IOException e) {
            throw new DocumentSignatureException(
                    "PDF verification failed: corrupt file or invalid format", e);
        }
    }

    private SignatureInfo buildSignatureInfo(String fieldName,
                                             SignatureUtil util,
                                             X509Certificate caCert) {
        PdfPKCS7 pkcs7 = util.readSignatureData(fieldName);

        boolean integrity;
        try {
            integrity = pkcs7.verifySignatureIntegrityAndAuthenticity();
        } catch (GeneralSecurityException e) {
            log.warn("Verification of signature '{}' failed: {}", fieldName, e.getMessage());
            integrity = false;
        }

        X509Certificate signerCert = (X509Certificate) pkcs7.getSigningCertificate();
        Calendar signCalendar = pkcs7.getSignDate();
        Instant signedAt = (signCalendar != null) ? signCalendar.toInstant() : null;

        String serial = signerCert.getSerialNumber().toString(16);
        String subjectDn = signerCert.getSubjectX500Principal().getName();
        String issuerDn = signerCert.getIssuerX500Principal().getName();
        String signerCn = extractCommonName(subjectDn);

        boolean trusted = isIssuedByCa(signerCert, caCert);

        // A serial number is only meaningful for certificates this CA actually issued
        CertificateStatus status = trusted
                ? certificateRepository.findBySerialNumber(serial)
                        .map(Certificate::getEffectiveStatus)
                        .orElse(null)
                : null;

        return new SignatureInfo(fieldName, signerCn, subjectDn, issuerDn,
                serial, signedAt, integrity, trusted, status);
    }

    /**
     * Checks that the certificate names this CA as issuer and that its signature
     * verifies with the CA public key. Matching the issuer name alone is not
     * enough: anyone can create a certificate with an arbitrary issuer DN.
     */
    private boolean isIssuedByCa(X509Certificate certificate, X509Certificate caCert) {
        if (!certificate.getIssuerX500Principal().equals(caCert.getSubjectX500Principal())) {
            return false;
        }
        try {
            certificate.verify(caCert.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            log.warn("Certificate claims this CA as issuer but its signature does not verify: serial={}",
                    certificate.getSerialNumber().toString(16));
            return false;
        }
    }

    /**
     * Extracts the CN value from an X.500 DN string.
     * Returns the raw DN if the CN cannot be isolated.
     */
    private String extractCommonName(String distinguishedName) {
        Matcher matcher = CN_PATTERN.matcher(distinguishedName);
        return matcher.find() ? matcher.group(1).trim() : distinguishedName;
    }
}
