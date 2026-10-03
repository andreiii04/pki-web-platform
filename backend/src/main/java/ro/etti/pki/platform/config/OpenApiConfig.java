package ro.etti.pki.platform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the generated Swagger UI.
 * <p>
 * Declares the {@value #BEARER_AUTH} scheme so protected endpoints can be tried
 * out from Swagger UI after pasting a JWT obtained from {@code /api/auth/login}.
 * </p>
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "PKI Web Platform API",
        version = "v1",
        description = "Private Certificate Authority, X.509 certificate issuance and PAdES PDF signing/verification.",
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    /** Name of the security scheme referenced by protected endpoints. */
    public static final String BEARER_AUTH = "bearerAuth";
}
