
package dev.kayquemiguel.aegis.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnProperty(
        name = "aegis.security.mode",
        havingValue = "database"
)
public class AdminBootstrapConfig {

    @Bean
    ApplicationRunner adminBootstrap(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,

            @Value("${aegis.bootstrap.admin.username:}")
            String username,

            @Value("${aegis.bootstrap.admin.password:}")
            String password
    ) {
        return args -> {
            boolean hasUsername = !username.isBlank();
            boolean hasPassword = !password.isBlank();

            if (hasUsername != hasPassword) {
                throw new IllegalStateException(
                        "Both bootstrap admin username "
                                + "and password are required"
                );
            }

            if (!hasUsername) {
                return;
            }

            if (username.length() > 64
                    || !username.matches(
                    "[A-Za-z0-9][A-Za-z0-9._-]*"
            )) {
                throw new IllegalArgumentException(
                        "Invalid bootstrap admin username"
                );
            }

            if (password.length() < 16) {
                throw new IllegalArgumentException(
                        "Bootstrap password must have "
                                + "at least 16 characters"
                );
            }

            Long existingUsers = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM aegis_users",
                    Long.class
            );

            if (existingUsers != null
                    && existingUsers > 0) {
                return;
            }

            // A insercao tambem verifica a ausencia
            // de usuarios, reduzindo a chance de
            // bootstrap duplicado.
            jdbc.update(
                    """
                    INSERT INTO aegis_users (
                        id,
                        username,
                        password_hash,
                        role,
                        enabled
                    )
                    SELECT ?, ?, ?, 'ADMIN', TRUE
                    WHERE NOT EXISTS (
                        SELECT 1 FROM aegis_users
                    )
                    """,
                    UUID.randomUUID(),
                    username,
                    passwordEncoder.encode(password)
            );
        };
    }
}
