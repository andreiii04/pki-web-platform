package ro.etti.pki.platform.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cryptographic parameters: RSA, signatures, AES.
 * <p>
 * Binds the {@code pki.crypto:} section of {@code application.yml}.
 * </p>
 *
 * @see <a href="https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final">NIST SP 800-57 Part 1 Rev. 5 — Key Management</a>
 */
public class CryptoProperties {

    /**
     * RSA key size (bits) for user keys.
     * <p>
     * Below 2048 is considered insecure per NIST SP 800-57; above 4096 adds
     * computational cost with no practical benefit in a demo context. The Root
     * CA is separate (4096) and does not use this value.
     * </p>
     */
    @Min(2048)
    @Max(4096)
    private int rsaKeySize;

    /**
     * Signature algorithm used to issue X.509 certificates and to sign
     * PDF documents.
     * <p>
     * Restricted to the RSA + SHA-2 families. SHA-1 is excluded for security
     * reasons (practical collisions have been demonstrated).
     * </p>
     */
    @NotBlank
    @Pattern(regexp = "^SHA(256|384|512)withRSA$")
    private String signatureAlgorithm;

    /**
     * AES-256 master key (Base64) used to encrypt users' private keys
     * before they are stored in the DB.
     * <p>
     * The check that it decodes to exactly 32 bytes (256 bits) happens in
     * {@code CryptoService} at startup; here only the Base64 format is validated.
     * </p>
     */
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9+/]+=*$")
    private String aesMasterKey;

    public CryptoProperties() {
    }

    public int getRsaKeySize() {
        return rsaKeySize;
    }

    public void setRsaKeySize(int rsaKeySize) {
        this.rsaKeySize = rsaKeySize;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getAesMasterKey() {
        return aesMasterKey;
    }

    public void setAesMasterKey(String aesMasterKey) {
        this.aesMasterKey = aesMasterKey;
    }
}
