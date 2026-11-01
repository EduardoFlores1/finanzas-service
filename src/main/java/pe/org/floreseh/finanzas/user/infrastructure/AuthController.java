package pe.org.floreseh.finanzas.user.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import pe.org.floreseh.finanzas.user.application.AuthService.RegistrationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.org.floreseh.finanzas.user.application.AuthService;
import pe.org.floreseh.finanzas.user.presentation.dto.AuthResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.LoginRequest;
import pe.org.floreseh.finanzas.user.presentation.dto.UserCreateRequest;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody UserCreateRequest request,
            HttpServletRequest httpRequest
            ) {
        RegistrationResult result = authService.register(request, true);

        return switch (result) {
            case RegistrationResult.Success(var authResponse) -> ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(authResponse);

            case RegistrationResult.EmailAlreadyExists(var email) -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(createProblem(
                            HttpStatus.CONFLICT,
                            "Correo ya registrado",
                            "El correo '%s' ya se encuentra asociado a una cuenta activa.".formatted(email),
                            "USER_EMAIL_ALREADY_EXISTS",
                            httpRequest
                    ));

            case RegistrationResult.InvalidRoleAssignment(var attemtedRole, var reason) -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(createProblem(
                            HttpStatus.FORBIDDEN,
                            "Asignación de rol no permitida:" + attemtedRole,
                            reason,
                            "AUTH_INVALID_ROLE_SELF_ASSIGNMENT",
                            httpRequest
                    ));
        };
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
            ) {
        return ResponseEntity.ok(authService.login(request));
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
