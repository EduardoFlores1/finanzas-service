package pe.org.floreseh.finanzas.core.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank(message = "La clave secreta JWT es obligatoria")
        @Size(min = 32, message = "La clave secreta debe tener al menos 32 caracteres (256 bits)")
        String secretKey,

        @NotNull(message = "El tiempo de expiracion es obligatorio")
        Duration expiration,

        @NotBlank(message = "El emisor (issuer) del token es obligatorio")
        String issuer
) {
}
