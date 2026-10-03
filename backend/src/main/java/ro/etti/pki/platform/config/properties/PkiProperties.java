package ro.etti.pki.platform.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Root properties for the PKI configuration.
 * <p>
 * Binds the {@code pki:} section of {@code application.yml} to a graph of
 * typed objects. Bean Validation runs when the Spring context starts, so any
 * missing or invalid value stops startup with a clear message.
 * </p>
 *
 * @see CaProperties
 * @see CryptoProperties
 * @see CertificateProperties
 */
@ConfigurationProperties(prefix = "pki")
@Validated
public class PkiProperties {

    @NotNull
    @Valid
    private CaProperties ca;

    @NotNull
    @Valid
    private CryptoProperties crypto;

    @NotNull
    @Valid
    private CertificateProperties certificate;

    public PkiProperties() {
    }

    public CaProperties getCa() {
        return ca;
    }

    public void setCa(CaProperties ca) {
        this.ca = ca;
    }

    public CryptoProperties getCrypto() {
        return crypto;
    }

    public void setCrypto(CryptoProperties crypto) {
        this.crypto = crypto;
    }

    public CertificateProperties getCertificate() {
        return certificate;
    }

    public void setCertificate(CertificateProperties certificate) {
        this.certificate = certificate;
    }
}
