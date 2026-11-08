
package dev.kayquemiguel.aegis.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AdminUserService {

    private static final Pattern IDENTIFIER =
            Pattern.compile(
                    "[A-Za-z0-9][A-Za-z0-9._-]{0,63}"
            );

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public AdminUserService(
            JdbcTemplate jdbc,
            PasswordEncoder encoder
    ) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    public record UserView(
            UUID id,
            String username,
            String role,
            boolean enabled,
            List<String> sensors
    ) {
    }

    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return jdbc.query(
                """
                SELECT id, username, role, enabled
                FROM aegis_users
                ORDER BY username
                LIMIT 100
                """,
                (rs, rowNum) -> {
                    UUID id = rs.getObject(
                            "id", UUID.class
                    );

                    return new UserView(
                            id,
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getBoolean("enabled"),
                            sensorsOf(id)
                    );
                }
        );
    }

    @Transactional
    public UserView createAnalyst(
            String username,
            String password,
            Set<String> sensors
    ) {
        validateIdentifier(username);
        validatePassword(password);
        validateSensors(sensors);

        UUID id = UUID.randomUUID();

        try {
            jdbc.update(
                    """
                    INSERT INTO aegis_users (
                        id, username, password_hash,
                        role, enabled
                    )
                    VALUES (?, ?, ?, 'ANALYST', TRUE)
                    """,
                    id,
                    username,
                    encoder.encode(password)
            );
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        insertSensors(id, sensors);

        return new UserView(
                id,
                username,
                "ANALYST",
                true,
                sensors.stream().sorted().toList()
        );
    }

    @Transactional
    public UserView replaceSensors(
            UUID userId,
            Set<String> sensors
    ) {
        validateSensors(sensors);

        requireAnalyst(userId);

        jdbc.update(
                """
                DELETE FROM aegis_user_sensors
                WHERE user_id = ?
                """,
                userId
        );

        insertSensors(userId, sensors);

        return findUser(userId);
    }

    @Transactional
    public UserView setAnalystEnabled(
            UUID userId,
            boolean enabled
    ) {
        int updated = jdbc.update(
                """
                UPDATE aegis_users
                SET enabled = ?
                WHERE id = ?
                  AND role = 'ANALYST'
                """,
                enabled,
                userId
        );

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Analyst not found"
            );
        }

        return findUser(userId);
    }

    @Transactional
    public void changeOwnPassword(
            String username,
            String currentPassword,
            String newPassword
    ) {
        validatePassword(newPassword);

        String existingHash;

        try {
            existingHash = jdbc.queryForObject(
                    """
                    SELECT password_hash
                    FROM aegis_users
                    WHERE username = ?
                      AND enabled = TRUE
                      AND role = 'ADMIN'
                    """,
                    String.class,
                    username
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active administrator required"
            );
        }

        if (!encoder.matches(
                currentPassword,
                existingHash
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        if (encoder.matches(
                newPassword,
                existingHash
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different"
            );
        }

        int updated = jdbc.update(
                """
                UPDATE aegis_users
                SET password_hash = ?
                WHERE username = ?
                  AND password_hash = ?
                  AND enabled = TRUE
                  AND role = 'ADMIN'
                """,
                encoder.encode(newPassword),
                username,
                existingHash
        );

        if (updated != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Password changed concurrently"
            );
        }
    }

    private UserView findUser(UUID userId) {
        try {
            return jdbc.queryForObject(
                    """
                    SELECT id, username, role, enabled
                    FROM aegis_users
                    WHERE id = ?
                    """,
                    (rs, rowNum) -> new UserView(
                            rs.getObject(
                                    "id", UUID.class
                            ),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getBoolean("enabled"),
                            sensorsOf(userId)
                    ),
                    userId
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }
    }

    private void requireAnalyst(UUID userId) {
        UserView user = findUser(userId);

        if (!"ANALYST".equals(user.role())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator cannot be modified here"
            );
        }
    }

    private List<String> sensorsOf(UUID userId) {
        return jdbc.queryForList(
                """
                SELECT sensor_id
                FROM aegis_user_sensors
                WHERE user_id = ?
                ORDER BY sensor_id
                """,
                String.class,
                userId
        );
    }

    private void insertSensors(
            UUID userId,
            Set<String> sensors
    ) {
        for (String sensorId : sensors) {
            jdbc.update(
                    """
                    INSERT INTO aegis_user_sensors (
                        user_id, sensor_id
                    )
                    VALUES (?, ?)
                    """,
                    userId,
                    sensorId
            );
        }
    }

    private static void validateIdentifier(
            String value
    ) {
        if (value == null
                || !IDENTIFIER.matcher(value).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid identifier"
            );
        }
    }

    private static void validateSensors(
            Set<String> sensors
    ) {
        if (sensors == null || sensors.size() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sensor collection"
            );
        }

        for (String sensor : sensors) {
            validateIdentifier(sensor);
        }
    }

    private static void validatePassword(
            String password
    ) {
        if (password == null
                || password.length() < 16
                || password.getBytes(
                StandardCharsets.UTF_8
        ).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must have at least 16 "
                            + "characters and at most "
                            + "72 UTF-8 bytes"
            );
        }
    }
}
