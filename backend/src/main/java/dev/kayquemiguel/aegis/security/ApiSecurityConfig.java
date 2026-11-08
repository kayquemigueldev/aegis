
package dev.kayquemiguel.aegis.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class ApiSecurityConfig {

    private static final String SENSOR_PATTERN =
            "[A-Za-z0-9][A-Za-z0-9._-]{0,63}";

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
                                        "/api/v1/admin/**"
                                )
                                .hasRole("ADMIN")
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/alerts",
                                        "/api/v1/alerts/**"
                                )
                                .hasAnyRole(
                                        "ANALYST",
                                        "ADMIN"
                                )
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
    @ConditionalOnProperty(
            name = "aegis.security.mode",
            havingValue = "local",
            matchIfMissing = true
    )
    UserDetailsService userDetailsService(
            @Value("${aegis.security.username:}")
            String username,

            @Value("${aegis.security.password:}")
            String password,

            @Value("${aegis.security.sensors:}")
            String sensors,

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

        Set<String> allowedSensors =
                Arrays.stream(sensors.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(
                                Collectors.toUnmodifiableSet()
                        );

        for (String sensorId : allowedSensors) {
            if (!sensorId.matches(SENSOR_PATTERN)) {
                throw new IllegalArgumentException(
                        "Invalid authorized sensor ID"
                );
            }
        }

        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_ANALYST");

        for (String sensorId : allowedSensors) {
            authorities.add(
                    SensorAccessPolicy.SENSOR_PREFIX
                            + sensorId
            );
        }

        UserDetails analyst = User
                .withUsername(username)
                .password(encoder.encode(password))
                .authorities(
                        authorities.toArray(String[]::new)
                )
                .build();

        return new InMemoryUserDetailsManager(analyst);
    }
}
