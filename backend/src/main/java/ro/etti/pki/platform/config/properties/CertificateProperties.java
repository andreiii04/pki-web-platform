package ro.etti.pki.platform.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Properties for the X.509 certificates issued to users.
 * <p>
 * Binds the {@code pki.certificate:} section of {@code application.yml}.
 * </p>
 */
public class CertificateProperties {

    /**
     * Validity period (days) of certificates issued to users.
     * The upper bound (3650 days ≈ 10 years) guards against misconfiguration
     * that would produce certificates with an excessive lifetime.
     */
    @Min(1)
    @Max(3650)
    private int validityDays;

    /**
     * Organization (O) value for the DN of issued certificates, unless
     * explicitly overridden at issuance.
     */
    @NotBlank
    private String organizationDefault;

    /**
     * Country (C) value for the DN of issued certificates.
     * ISO 3166-1 alpha-2 code (two uppercase letters, e.g. RO, US, DE).
     */
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$")
    private String country;

    public CertificateProperties() {
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public String getOrganizationDefault() {
        return organizationDefault;
    }

    public void setOrganizationDefault(String organizationDefault) {
        this.organizationDefault = organizationDefault;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
