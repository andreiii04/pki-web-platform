package ro.etti.pki.platform.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.etti.pki.platform.config.properties.PkiProperties;
import ro.etti.pki.platform.dto.response.SignatureInfo;
import ro.etti.pki.platform.dto.response.VerificationResponse;
import ro.etti.pki.platform.entity.Certificate;
import ro.etti.pki.platform.entity.CertificateStatus;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.entity.UserRole;
import ro.etti.pki.platform.repository.CertificateRepository;
import ro.etti.pki.platform.service.ca.CertificateAuthorityService;
import ro.etti.pki.platform.service.ca.TestCaServices;
import ro.etti.pki.platform.service.crypto.TestCryptoServices;
import ro.etti.pki.platform.support.TestPki;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentSignatureServiceTest {

    @TempDir
    Path tempDir;

    private CertificateRepository repository;
    private KeyManagerService keyManager;
    private DocumentSignatureService signatureService;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        Path keystore = TestPki.createCaKeystore(tempDir.resolve("ca.p12"), TestPki.CA_SUBJECT);
        PkiProperties properties = TestPki.properties(keystore);
        CertificateAuthorityService caService = TestCaServices.loaded(properties);

        repository = mock(CertificateRepository.class);
        when(repository.save(any(Certificate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findBySerialNumber(anyString())).thenReturn(Optional.empty());

        keyManager = new KeyManagerService(
                TestCryptoServices.initialized(properties), caService, repository, properties);
        signatureService = new DocumentSignatureService(keyManager, caService, repository);
        user = new User("jane", "jane@example.com", "hash", UserRole.USER);
    }

    @Test
    void signedPdfVerifiesAsValidAndTrusted() {
        Certificate certificate = keyManager.generateAndStore(user, "Jane Doe", "Example Org");
        when(repository.findBySerialNumber(certificate.getSerialNumber()))
                .thenReturn(Optional.of(certificate));

        byte[] signed = signatureService.sign(blankPdf(), certificate, null, null);
        VerificationResponse result = signatureService.verify(signed);

        assertThat(result.signed()).isTrue();
        assertThat(result.allSignaturesValid()).isTrue();
        assertThat(result.signaturesCount()).isEqualTo(1);
        SignatureInfo info = result.signatures().get(0);
        assertThat(info.signerCommonName()).isEqualTo("Jane Doe");
        assertThat(info.integrityValid()).isTrue();
        assertThat(info.trusted()).isTrue();
        assertThat(info.certificateStatus()).isEqualTo(CertificateStatus.ACTIVE);
    }

    @Test
    void unsignedPdfIsReportedAsNotSigned() {
        VerificationResponse result = signatureService.verify(blankPdf());

        assertThat(result.signed()).isFalse();
        assertThat(result.signaturesCount()).isZero();
        assertThat(result.messages()).containsExactly("The document contains no signatures");
    }

    @Test
    void certificateFromImpostorCaWithSameNameIsNotTrusted() throws Exception {
        // An attacker-controlled CA reusing the exact subject DN of the real CA
        Path fakeKeystore = TestPki.createCaKeystore(tempDir.resolve("fake.p12"), TestPki.CA_SUBJECT);
        PkiProperties fakeProperties = TestPki.properties(fakeKeystore);
        CertificateAuthorityService fakeCa = TestCaServices.loaded(fakeProperties);
        KeyManagerService fakeKeyManager = new KeyManagerService(
                TestCryptoServices.initialized(fakeProperties), fakeCa, repository, fakeProperties);
        DocumentSignatureService fakeSigner = new DocumentSignatureService(fakeKeyManager, fakeCa, repository);

        Certificate forged = fakeKeyManager.generateAndStore(user, "Jane Doe", "Example Org");
        when(repository.findBySerialNumber(forged.getSerialNumber())).thenReturn(Optional.of(forged));
        byte[] signed = fakeSigner.sign(blankPdf(), forged, null, null);

        SignatureInfo info = signatureService.verify(signed).signatures().get(0);

        assertThat(info.integrityValid()).isTrue();
        assertThat(info.trusted()).isFalse();
        assertThat(info.certificateStatus()).isNull();
    }

    private static byte[] blankPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument doc = new PdfDocument(new PdfWriter(out))) {
            doc.addNewPage();
        }
        return out.toByteArray();
    }
}
