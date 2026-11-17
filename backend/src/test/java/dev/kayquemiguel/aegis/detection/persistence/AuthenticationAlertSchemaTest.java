package dev.kayquemiguel.aegis.detection.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthenticationAlertSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldPersistAuthenticationAlertMetadata() {
        UUID id = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO detection_alerts (
                    id, rule_id, sensor_id, source_ip,
                    destination_ip, detected_at,
                    distinct_ports, detection_threshold,
                    description, metric_type, observed_count,
                    auth_username, auth_source_system
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "NET-003",
                "sensor-test",
                "192.0.2.10",
                null,
                Timestamp.from(
                        Instant.parse("2026-11-16T10:00:00Z")
                ),
                0,
                5,
                "Repeated authentication failures",
                "AUTHENTICATION_FAILURES",
                5,
                "admin",
                "auth-log"
        );

        String username = jdbc.queryForObject(
                "SELECT auth_username FROM detection_alerts WHERE id = ?",
                String.class,
                id
        );

        String metric = jdbc.queryForObject(
                "SELECT metric_type FROM detection_alerts WHERE id = ?",
                String.class,
                id
        );

        assertEquals("admin", username);
        assertEquals("AUTHENTICATION_FAILURES", metric);
    }

    @Test
    void shouldKeepExistingNetworkAlertFormat() {
        UUID id = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO detection_alerts (
                    id, rule_id, sensor_id, source_ip,
                    destination_ip, detected_at,
                    distinct_ports, detection_threshold,
                    description, metric_type, observed_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "NET-001",
                "sensor-test",
                "192.0.2.10",
                "192.0.2.20",
                Timestamp.from(
                        Instant.parse("2026-11-16T10:00:00Z")
                ),
                3,
                3,
                "Possible vertical scan",
                "DISTINCT_DESTINATION_PORTS",
                3
        );

        Integer count = jdbc.queryForObject(
                "SELECT observed_count FROM detection_alerts WHERE id = ?",
                Integer.class,
                id
        );

        assertEquals(3, count);
    }
}
