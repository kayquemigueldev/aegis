
package dev.kayquemiguel.aegis.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnProperty(
        name = "aegis.security.mode",
        havingValue = "database"
)
public class DatabaseUserDetailsService
        implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public DatabaseUserDetailsService(
            JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        List<DatabaseUser> users = jdbc.query(
                """
                SELECT id, username, password_hash,
                       role, enabled
                FROM aegis_users
                WHERE username = ?
                """,
                (rs, rowNum) -> new DatabaseUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("enabled")
                ),
                username
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                    "Invalid credentials"
            );
        }

        DatabaseUser account = users.get(0);

        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + account.role()
                )
        );

        if ("ANALYST".equals(account.role())) {
            List<String> sensors = jdbc.queryForList(
                    """
                    SELECT sensor_id
                    FROM aegis_user_sensors
                    WHERE user_id = ?
                    """,
                    String.class,
                    account.id()
            );

            for (String sensorId : sensors) {
                authorities.add(
                        new SimpleGrantedAuthority(
                                SensorAccessPolicy.SENSOR_PREFIX
                                        + sensorId
                        )
                );
            }
        }

        return User.withUsername(account.username())
                .password(account.passwordHash())
                .authorities(
                        authorities.toArray(
                                SimpleGrantedAuthority[]::new
                        )
                )
                .disabled(!account.enabled())
                .build();
    }

    private record DatabaseUser(
            UUID id,
            String username,
            String passwordHash,
            String role,
            boolean enabled
    ) {
    }
}
