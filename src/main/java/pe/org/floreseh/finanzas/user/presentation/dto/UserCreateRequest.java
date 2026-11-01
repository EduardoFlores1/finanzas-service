package pe.org.floreseh.finanzas.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.org.floreseh.finanzas.user.domain.Role;

public record UserCreateRequest(
        @NotBlank(message = "El email no puede ser vacío")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Role role
) {
        public UserCreateRequest {
                if (email != null) {
                        email = email.strip().toLowerCase();
                }
        }
}
