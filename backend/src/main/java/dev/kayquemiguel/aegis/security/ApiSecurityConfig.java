
package dev.kayquemiguel.aegis.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;


@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class ApiSecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/alerts",
                                        "/api/v1/alerts/**"
                                )
                                .hasRole("ANALYST")
                                .anyRequest()
                                .denyAll()
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${aegis.security.username:}")
            String username,

            @Value("${aegis.security.password:}")
            String password,

            PasswordEncoder encoder
    ) {

        boolean hasUsername = !username.isBlank();
        boolean hasPassword = !password.isBlank();

        if (hasUsername != hasPassword) {
            throw new IllegalStateException(
                    "Configure both AEGIS_API_USERNAME "
                            + "and AEGIS_API_PASSWORD"
            );
        }

        if (!hasUsername) {
            // Sem configuracao, nao cria usuario
            // nem permite acesso anonimo.
            return new InMemoryUserDetailsManager();
        }

        if (username.length() > 64
                || !username.matches(
                "[A-Za-z0-9][A-Za-z0-9._-]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid API username"
            );
        }

        if (password.length() < 16) {
            throw new IllegalArgumentException(
                    "API password must have at least "
                            + "16 characters"
            );
        }

        UserDetails analyst = User.withUsername(username)
                .password(encoder.encode(password))
                .roles("ANALYST")
                .build();

        return new InMemoryUserDetailsManager(analyst);
    }
}
