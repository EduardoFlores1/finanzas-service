package pe.org.floreseh.finanzas.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(Exception ex, HttpServletRequest request) {

        String incidentId = UUID.randomUUID().toString();

        log.error("Error no controlado [IncidentId: {}] en la ruta [{} {}]: ",
                incidentId, request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno inesperado en el servidor. Por favor, contacte a soporte con el código de incidente."
        );
        problem.setTitle("Error interno del servidor");
        problem.setInstance(URI.create(request.getRequestURI()));

        problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problem.setProperty("incidentId", incidentId);
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado",
                ex.getMessage(),
                ex.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(
                        Collectors.toMap(
                                FieldError::getField,
                                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido",
                                (msg1, msg2) -> msg1 + "; " + msg2
                        )
                );

        String primaryMessage = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Error de validación en los datos enviados"
                : ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();

        ProblemDetail problemDetail = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Datos de entrada inválidos",
                primaryMessage,
                "VALIDATION_ERROR",
                request
        );

        problemDetail.setProperty("invalidFields", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildProblem(
          HttpStatus.CONFLICT,
          "Conflicto de concurrencia detectado",
          "El registro fue modificado por otro operador simultaneamente. Recargue los datos e intente nuevamente.",
                "CONCURRENCY_CONFICLT",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Regla de negocio infringida",
                ex.getMessage(),
                "BUSINESS_RULE_VIOLATION",
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas",
                "El correo electrónico o la contraseña proporcionados son incorrectos.",
                "AUTH_BAD_CREDENTIALS",
                request
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabledUser(DisabledException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.FORBIDDEN,
                "Cuenta desactivada",
                "Esta cuenta ha sido dada de baja por motivos de seguridad o auditoría.",
                "AUTH_ACCOUNT_DISABLED",
                request
        );
    }

    private ProblemDetail buildProblem(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request
    ) {
        ProblemDetail problem  = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
