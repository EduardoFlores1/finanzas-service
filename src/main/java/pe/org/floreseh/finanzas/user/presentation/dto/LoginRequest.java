package pe.org.floreseh.finanzas.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "EL email es obligatorio")
        @Email(message = "El formato de email es inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
    public LoginRequest {
        if(email != null) {
            email = email.strip().toLowerCase();
        }
    }
}
