package pe.org.floreseh.finanzas.user.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import pe.org.floreseh.finanzas.user.domain.Role;

public record UserRoleUpdateRequest(
        @NotNull(message = "El nuevo rol es obligatorio")
        Role newRole,

        @NotNull(message = "La versión del registro es obligatoria para el control de concurrencia")
        @PositiveOrZero(message = "La versión no puede ser negativa")
        Long version
) {
}
