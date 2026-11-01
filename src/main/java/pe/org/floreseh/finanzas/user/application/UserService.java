package pe.org.floreseh.finanzas.user.application;

import jakarta.annotation.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.floreseh.finanzas.core.exception.ResourceNotFoundException;
import pe.org.floreseh.finanzas.user.domain.Role;
import pe.org.floreseh.finanzas.user.domain.User;
import pe.org.floreseh.finanzas.user.infrastructure.UserRepository;
import pe.org.floreseh.finanzas.user.presentation.dto.UserAuditSummaryResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.UserResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.UserRoleUpdateRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

@Service
@Transactional(readOnly = true)
public class UserService {

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public sealed interface RoleUpdateResult {
        record Updated(UserResponse userResponse) implements RoleUpdateResult {}
        record SelfModificationForbidden(String reason) implements RoleUpdateResult {}
    }

    private final UserRepository userRepository;

    public Page<UserResponse> findAllActive(@Nullable Role roleFilter, Pageable pageable) {
        return Optional.ofNullable(roleFilter)
                .map(role -> userRepository.findAllByRoleAndIsActiveTrue(role, pageable))
                .orElseGet(() -> userRepository.findAllByIsActiveTrue(pageable))
                .map(UserResponse::fromEntity);
    }

    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    @Transactional
    public RoleUpdateResult updateRole(UUID targetUserId, UserRoleUpdateRequest request, String actorEmail) {
        User user =  userRepository.findById(targetUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", targetUserId));

        if(user.getEmail().equalsIgnoreCase(actorEmail)) {
            return new RoleUpdateResult.SelfModificationForbidden("Por políticas de auditoria (Segregación de Funciones), no puede modificar su propoio rol.");
        }

        if(!Objects.equals(user.getVersion(), request.version())) {
            throw new OptimisticLockingFailureException("El usuario fue modificado por otra transacción. Versión esperada: " + request.version() + ", versión actual: " + user.getVersion());
        }

        user.changeRole(request.newRole());

        return new RoleUpdateResult.Updated(UserResponse.fromEntity(user));
    }

    @Transactional
    public void deactivateUser(UUID targetUserId, String actorEmail) {
        User user =  userRepository.findById(targetUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", targetUserId));

        if(user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new IllegalArgumentException("No puede desactivar su propia cuenta.");
        }

        user.deactivate();
    }

    public UserAuditSummaryResponse generateActiveUserAudit(int bachSize) {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();

        record Metrics(long total, Map<Role, Long> byRole) {}

        Metrics metrics = activeUsers.stream().collect(
                Collectors.teeing(
                        Collectors.counting(),
                        Collectors.groupingBy(User::getRole, Collectors.counting()),
                        Metrics::new
                )
        );

        int safeBatchSize = Math.max(1, bachSize);
        List<List<String>> batches = activeUsers.stream()
                .map(User::getEmail)
                .gather(Gatherers.windowFixed(safeBatchSize))
                .toList();

        return new UserAuditSummaryResponse(
                metrics.total,
                metrics.byRole(),
                batches
        );
    }
}
