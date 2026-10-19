package pe.org.floreseh.finanzas.user.presentation.dto;

import pe.org.floreseh.finanzas.user.domain.Role;
import pe.org.floreseh.finanzas.user.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        boolean isActive,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
