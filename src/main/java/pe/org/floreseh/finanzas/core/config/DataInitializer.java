package pe.org.floreseh.finanzas.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.org.floreseh.finanzas.user.domain.Role;
import pe.org.floreseh.finanzas.user.domain.User;
import pe.org.floreseh.finanzas.user.infrastructure.UserRepository;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!prod")
    public ApplicationRunner seedMasterAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return _ -> {
            String adminEmail = "admin@finanzas.pe";

            if (!userRepository.existsByEmail(adminEmail)) {
                var masterAdmin = new User(
                        adminEmail,
                        passwordEncoder.encode("AdminFinanzas2026!"),
                        Role.ADMIN
                );
                userRepository.save(masterAdmin);
                log.info("Usuario ADMIN semilla creado exitosamente: {}", adminEmail);
            }
        };
    }
}
