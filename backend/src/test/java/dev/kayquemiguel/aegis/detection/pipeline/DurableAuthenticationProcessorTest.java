package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DurableAuthenticationProcessorTest {

    @Autowired
    DurableAuthenticationProcessor processor;

    @Autowired
    JdbcTemplate jdbc;

    private final List<String> sensors = new ArrayList<>();

    private String sensor() {
        String id = "durable-" + UUID.randomUUID();
        sensors.add(id);
        return id;
    }

    private AuthenticationEvent failure(
            String sensorId,
            int seconds
    ) {
        Instant base =
                Instant.parse("2032-01-01T10:00:00Z");

        return new AuthenticationEvent(
                UUID.randomUUID(),
                base.plusSeconds(seconds),
                base.plusSeconds(seconds + 1),
                "192.0.2.10",
                "admin",
                "auth-log",
                sensorId,
                AuthenticationOutcome.FAILURE
        );
    }

    @AfterEach
    void cleanup() {
        for (String sensorId : sensors) {
            jdbc.update(
                    "DELETE FROM auth_event_inbox WHERE sensor_id = ?",
                    sensorId
            );
            jdbc.update(
                    "DELETE FROM detection_alerts "
                    + "WHERE sensor_id = ? AND rule_id = 'NET-003'",
                    sensorId
            );
        }
    }

    @Test
    void shouldDetectFiveFailuresAndPersistEveryEvent() {
        String sensorId = sensor();

        for (int i = 0; i < 4; i++) {
            assertTrue(
                    processor.process(failure(sensorId, i))
                            .isEmpty()
            );
        }

        UUID alertId = processor.process(
                failure(sensorId, 4)
        ).orElseThrow();

        assertNotNull(alertId);

        Integer events = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE sensor_id = ?",
                Integer.class,
                sensorId
        );

        Integer processed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND processed = TRUE",
                Integer.class,
                sensorId
        );

        assertEquals(5, events);
        assertEquals(5, processed);
    }

    @Test
    void shouldNotDuplicateAlertOnResubmission() {
        String sensorId = sensor();

        for (int i = 0; i < 4; i++) {
            processor.process(failure(sensorId, i));
        }

        AuthenticationEvent fifth = failure(sensorId, 4);

        UUID first = processor.process(fifth).orElseThrow();
        UUID second = processor.process(fifth).orElseThrow();

        assertEquals(first, second);

        Integer alerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? AND rule_id = 'NET-003'",
                Integer.class,
                sensorId
        );

        assertEquals(1, alerts);
    }

    @Test
    void shouldRejectConflictingResubmission() {
        String sensorId = sensor();
        AuthenticationEvent original = failure(sensorId, 0);

        processor.process(original);

        AuthenticationEvent changed = new AuthenticationEvent(
                original.id(),
                original.occurredAt(),
                original.ingestedAt(),
                original.sourceIp(),
                "different-user",
                original.sourceSystem(),
                original.sensorId(),
                original.outcome()
        );

        assertThrows(
                RuntimeException.class,
                () -> processor.process(changed)
        );

        Integer events = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE sensor_id = ?",
                Integer.class,
                sensorId
        );

        assertEquals(1, events);
    }
}
