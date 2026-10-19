package pe.org.floreseh.finanzas.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.org.floreseh.finanzas.user.domain.Role;

public record UserCreateRequest(
        @NotBlank(message = "El email no puede ser vacío")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
