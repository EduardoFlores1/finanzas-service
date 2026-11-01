package pe.org.floreseh.finanzas.user.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import pe.org.floreseh.finanzas.user.application.AuthService.RegistrationResult;
import pe.org.floreseh.finanzas.user.application.UserService.RoleUpdateResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.org.floreseh.finanzas.core.security.SecurityUser;
import pe.org.floreseh.finanzas.user.application.AuthService;
import pe.org.floreseh.finanzas.user.application.UserService;
import pe.org.floreseh.finanzas.user.domain.Role;
import pe.org.floreseh.finanzas.user.presentation.dto.UserAuditSummaryResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.UserCreateRequest;
import pe.org.floreseh.finanzas.user.presentation.dto.UserResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.UserRoleUpdateRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal SecurityUser securityUser
            ) {

        return ResponseEntity.ok(UserResponse.fromEntity(securityUser.user()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<?> createUserByAdmin(
            @Valid @RequestBody UserCreateRequest request,
            HttpServletRequest httpRequest
            ) {

        return switch (authService.register(request, false)) {
            case RegistrationResult.Success(var authResponse) -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(authResponse.user());

            case RegistrationResult.EmailAlreadyExists(var email) -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createProblem(
                            HttpStatus.CONFLICT,
                            "Conflicto de unicidad",
                            "El correo '%s' ya esta registrado.".formatted(email),
                            "USER_EMAIL_ALREADY_EXISTS",
                            httpRequest
                    ));

            case RegistrationResult.InvalidRoleAssignment(_, var reason) -> ResponseEntity
                    .badRequest()
                    .body(reason);
        };
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<Page<UserResponse>> findAllActive(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10, sort = "createdAt")Pageable pageable
            ) {

        return ResponseEntity.ok(userService.findAllActive(role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {

        return ResponseEntity.ok(userService.findById(id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<?> findById(
            @PathVariable UUID id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            @AuthenticationPrincipal SecurityUser currentUser,
            HttpServletRequest httpRequest
            ) {

        RoleUpdateResult result = userService.updateRole(id, request, currentUser.getUsername());

        return switch (result) {
            case RoleUpdateResult.Updated(var updatedUser) -> ResponseEntity
                    .ok(updatedUser);

            case RoleUpdateResult.SelfModificationForbidden(var reason) -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(createProblem(
                            HttpStatus.FORBIDDEN,
                            "Violación en la Segregación de Funciones (SoD)",
                            reason,
                            "SOD_SELF_ROLE_MODIFICATION",
                            httpRequest
                    ));
        };
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {

        userService.deactivateUser(id, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit/summary")
    @PreAuthorize("hasAuthority('audit:read')")
    public ResponseEntity<UserAuditSummaryResponse> getAuditSummary(
            @RequestParam(defaultValue = "2") int batchSize
    ) {

        return ResponseEntity.ok(userService.generateActiveUserAudit(batchSize));
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
