package ro.etti.pki.platform;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.security.Provider;
import java.security.Security;

/**
 * Spring Boot entry point for the PKI platform.
 * <p>
 * Registers Bouncy Castle as a JCA Security Provider before Spring boots
 * (in a static initializer),
 * so that all cryptographic components (CryptoService, CertificateAuthorityService)
 * can explicitly use the "BC" provider via {@code getInstance("...", "BC")}.
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan("ro.etti.pki.platform.config.properties")
public class PkiPlatformApplication {

    private static final Logger log = LoggerFactory.getLogger(PkiPlatformApplication.class);

    // Runs when the class is loaded, so the provider is also registered when the
    // context is bootstrapped without main() (e.g. @SpringBootTest).
    static {
        registerBouncyCastle();
    }

    public static void main(String[] args) {
        SpringApplication.run(PkiPlatformApplication.class, args);
    }

    /**
     * Registers Bouncy Castle as a JCA Security Provider.
     * <p>
     * Required for:
     * <ul>
     *   <li>{@code KeyPairGenerator.getInstance("RSA", "BC")}</li>
     *   <li>{@code Cipher.getInstance("AES/GCM/NoPadding", "BC")}</li>
     *   <li>Building X.509 v3 certificates with {@code X509v3CertificateBuilder} (bcpkix)</li>
     * </ul>
     * Idempotent: if the "BC" provider is already registered (e.g. on a Spring DevTools
     * restart where the JVM stays alive), the second call is silently skipped.
     * </p>
     */
    private static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Checks at context startup that the Bouncy Castle provider is active.
     * <p>
     * Runs once the application has started, so the log line appears next to the
     * "Started PkiPlatformApplication" message using the configured pattern.
     * If the provider is missing from JCA (e.g. blocked by module restrictions), it
     * throws {@link IllegalStateException} to fail fast at startup.
     * </p>
     */
    @Bean
    ApplicationRunner bouncyCastleStartupCheck() {
        return args -> {
            Provider bc = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (bc == null) {
                throw new IllegalStateException(
                        "Bouncy Castle Security Provider is not registered — check PkiPlatformApplication#registerBouncyCastle()");
            }
            log.info("Bouncy Castle Security Provider active: name={}, version={}",
                    bc.getName(), bc.getVersionStr());
        };
    }

}
