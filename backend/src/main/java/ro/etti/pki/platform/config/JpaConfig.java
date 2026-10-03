package ro.etti.pki.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Application-wide JPA configuration.
 * <p>
 * Enables automatic timestamp auditing: fields annotated with
 * {@link org.springframework.data.annotation.CreatedDate} and
 * {@link org.springframework.data.annotation.LastModifiedDate}
 * are populated automatically on {@code save()}.
 * </p>
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
