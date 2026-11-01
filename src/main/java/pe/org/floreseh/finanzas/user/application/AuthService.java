package pe.org.floreseh.finanzas.user.application;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.org.floreseh.finanzas.core.config.JwtProperties;
import pe.org.floreseh.finanzas.core.security.JwtService;
import pe.org.floreseh.finanzas.core.security.SecurityUser;
import pe.org.floreseh.finanzas.user.domain.Role;
import pe.org.floreseh.finanzas.user.domain.User;
import pe.org.floreseh.finanzas.user.infrastructure.UserRepository;
import pe.org.floreseh.finanzas.user.presentation.dto.AuthResponse;
import pe.org.floreseh.finanzas.user.presentation.dto.LoginRequest;
import pe.org.floreseh.finanzas.user.presentation.dto.UserCreateRequest;
import pe.org.floreseh.finanzas.user.presentation.dto.UserResponse;

import java.time.Instant;

@Service
public class AuthService {

    public sealed interface RegistrationResult {
        record Success(AuthResponse authResponse) implements RegistrationResult{}
        record EmailAlreadyExists(String email) implements RegistrationResult {}
        record InvalidRoleAssignment(Role attemptedRole, String reason) implements RegistrationResult {}
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegistrationResult register(UserCreateRequest request, boolean isPublicSelfRegistration) {
        if(isPublicSelfRegistration && request.role() != Role.CUSTOMER) {
            return new RegistrationResult.InvalidRoleAssignment(
                    request.role(),
                    "El auto-registro público solo permite asignar el rol CUSTOMER"
            );
        }

        if(userRepository.existsByEmail(request.email())) {
            return new RegistrationResult.EmailAlreadyExists(request.email());
        }

        var newUser = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );

        User savvedUser = userRepository.save(newUser);
        return new RegistrationResult.Success(buildAuthResponse(new SecurityUser(savvedUser)));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        if(authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return buildAuthResponse(securityUser);
        }

        throw new IllegalArgumentException("El principal de seguridad no es del tipo esperado SecurityUser");
    }

    private AuthResponse buildAuthResponse(SecurityUser securityUser) {
        String token = jwtService.generateToken(securityUser);
        Instant expiresAt = Instant.now().plus(jwtProperties.expiration());
        return AuthResponse.Bearer(token, expiresAt, UserResponse.fromEntity(securityUser.user()));
    }
}
