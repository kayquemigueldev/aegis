package dev.kayquemiguel.aegis.detection.persistence;

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
class AuthenticationEventDurabilityTest {

    @Autowired
    private AuthenticationEventInbox inbox;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<String> testSensors =
            new ArrayList<>();

    private AuthenticationEvent event() {
        String sensorId =
                "durability-" + UUID.randomUUID();

        testSensors.add(sensorId);

        Instant occurredAt =
                Instant.parse("2026-11-17T10:00:00Z");

        return new AuthenticationEvent(
                UUID.randomUUID(),
                occurredAt,
                occurredAt.plusSeconds(1),
                "192.0.2.10",
                "admin",
                "auth-log",
                sensorId,
                AuthenticationOutcome.FAILURE
        );
    }

    @AfterEach
    void cleanup() {
        for (String sensorId : testSensors) {
            jdbc.update(
                    "DELETE FROM auth_event_inbox "
                    + "WHERE sensor_id = ?",
                    sensorId
            );
        }
    }

    @Test
    void shouldKeepCommittedEventAcrossRepositoryInstances() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        // A chamada anterior ja concluiu sua transacao.
        AuthenticationEventInbox restarted =
                new AuthenticationEventInbox(jdbc);

        assertTrue(
                restarted.findPending(1000)
                        .contains(original)
        );

        assertFalse(restarted.record(original));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Integer.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(1, count);
    }

    @Test
    void shouldKeepProcessingAcknowledgementCommitted() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        assertTrue(inbox.markProcessed(
                original.sensorId(),
                original.id(),
                null
        ));

        AuthenticationEventInbox restarted =
                new AuthenticationEventInbox(jdbc);

        assertFalse(
                restarted.findPending(1000)
                        .contains(original)
        );

        Boolean processed = jdbc.queryForObject(
                "SELECT processed FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Boolean.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(Boolean.TRUE, processed);

        assertFalse(restarted.record(original));

        assertFalse(restarted.markProcessed(
                original.sensorId(),
                original.id(),
                null
        ));
    }
}
