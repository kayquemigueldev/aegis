package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthenticationEventInboxTest {

    @Autowired
    private AuthenticationEventInbox inbox;

    @Autowired
    private JdbcTemplate jdbc;

    private AuthenticationEvent event(
            UUID id,
            String sensorId,
            String username
    ) {
        Instant base =
                Instant.parse("2026-11-17T10:00:00Z");

        return new AuthenticationEvent(
                id,
                base,
                base.plusSeconds(1),
                "192.0.2.10",
                username,
                "auth-log",
                sensorId,
                AuthenticationOutcome.FAILURE
        );
    }

    private int count(UUID id, String sensorId) {
        Integer result = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_event_inbox
                WHERE event_id = ?
                  AND sensor_id = ?
                """,
                Integer.class,
                id,
                sensorId
        );

        return result == null ? 0 : result;
    }

    @Test
    void shouldPersistEventOnlyOnce() {
        UUID id = UUID.randomUUID();

        AuthenticationEvent event =
                event(id, "sensor-001", "admin");

        assertTrue(inbox.record(event));
        assertFalse(inbox.record(event));

        assertEquals(1, count(id, "sensor-001"));
    }

    @Test
    void shouldIsolateEventsBySensor() {
        UUID sharedId = UUID.randomUUID();

        assertTrue(inbox.record(
                event(sharedId, "sensor-001", "admin")
        ));

        assertTrue(inbox.record(
                event(sharedId, "sensor-002", "admin")
        ));

        assertEquals(
                1, count(sharedId, "sensor-001")
        );
        assertEquals(
                1, count(sharedId, "sensor-002")
        );
    }

    @Test
    void shouldRejectConflictingPayloadForSameId() {
        UUID id = UUID.randomUUID();

        assertTrue(inbox.record(
                event(id, "sensor-001", "admin")
        ));

        AuthenticationEventConflictException exception =
                assertThrows(
                        AuthenticationEventConflictException.class,
                        () -> inbox.record(
                                event(id, "sensor-001", "root")
                        )
                );

        assertEquals(
                "Conflicting authentication event ID",
                exception.getMessage()
        );

        assertEquals(1, count(id, "sensor-001"));
    }

    @Test
    void shouldRecoverPendingEventFromDatabase() {
        AuthenticationEvent original =
                event(
                        UUID.randomUUID(),
                        "sensor-restart",
                        "admin"
                );

        assertTrue(inbox.record(original));

        // Simula a criacao de uma nova instancia
        // da camada de acesso aos dados.
        AuthenticationEventInbox restarted =
                new AuthenticationEventInbox(jdbc);

        assertTrue(
                restarted.findPending(100).contains(original)
        );
    }

    @Test
    void shouldMarkEventProcessedOnlyOnce() {
        AuthenticationEvent original =
                event(
                        UUID.randomUUID(),
                        "sensor-ack",
                        "admin"
                );

        assertTrue(inbox.record(original));

        assertTrue(
                inbox.markProcessed(
                        original.sensorId(),
                        original.id(),
                        null
                )
        );

        assertFalse(
                inbox.markProcessed(
                        original.sensorId(),
                        original.id(),
                        null
                )
        );

        assertFalse(
                inbox.findPending(100).contains(original)
        );

        assertFalse(inbox.record(original));
    }

    @Test
    void shouldLimitPendingBatch() {
        String sensorId =
                "sensor-batch-" + UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            assertTrue(inbox.record(
                    event(
                            UUID.randomUUID(),
                            sensorId,
                            "admin"
                    )
            ));
        }

        var pending = inbox.findPending(2);

        assertEquals(2, pending.size());

        assertThrows(
                AuthenticationEventLimitException.class,
                () -> inbox.findPending(0)
        );

        assertThrows(
                AuthenticationEventLimitException.class,
                () -> inbox.findPending(1001)
        );
    }


    @Test
    void shouldPageReplayHistoryWithoutRepeatingEvents() {
        String sensor = "replay-" + UUID.randomUUID();
        java.time.Instant base =
                java.time.Instant.parse("2030-01-01T10:00:00Z");

        java.util.List<AuthenticationEvent> originals =
                new java.util.ArrayList<>();

        for (int i = 0; i < 3; i++) {
            AuthenticationEvent current =
                    new AuthenticationEvent(
                            UUID.randomUUID(),
                            base.plusSeconds(i),
                            base.plusSeconds(i),
                            "192.0.2.10",
                            "admin",
                            "auth-log",
                            sensor,
                            AuthenticationOutcome.FAILURE
                    );

            assertTrue(inbox.record(current));
            originals.add(current);
        }

        var firstPage = inbox.findReplayPage(
                null, null, null, 1000
        );

        assertTrue(firstPage.containsAll(originals));

        AuthenticationEvent last =
                originals.get(0);

        var nextPage = inbox.findReplayPage(
                last.ingestedAt(),
                last.sensorId(),
                last.id(),
                2
        );

        assertEquals(
                originals.subList(1, 3),
                nextPage
        );
    }

    @Test
    void shouldRebuildDetectionFromStoredEvents() {
        String sensor = "rebuild-" + UUID.randomUUID();
        java.time.Instant base =
                java.time.Instant.parse("2031-01-01T10:00:00Z");

        java.util.List<AuthenticationEvent> originals =
                new java.util.ArrayList<>();

        for (int i = 0; i < 5; i++) {
            AuthenticationEvent current =
                    new AuthenticationEvent(
                            UUID.randomUUID(),
                            base.plusSeconds(i),
                            base.plusSeconds(i + 1),
                            "192.0.2.10",
                            "admin",
                            "auth-log",
                            sensor,
                            AuthenticationOutcome.FAILURE
                    );

            assertTrue(inbox.record(current));
            originals.add(current);

            if (i < 4) {
                assertTrue(inbox.markProcessed(
                        current.sensorId(),
                        current.id(),
                        null
                ));
            }
        }

        var detector =
                new dev.kayquemiguel.aegis.detection
                        .BruteForceDetector(
                                java.time.Duration.ofSeconds(60),
                                5
                        );

        java.util.List<AuthenticationEvent> history =
                inbox.findReplayPage(
                        null, null, null, 1000
                );

        int generatedAlerts = 0;

        for (AuthenticationEvent current : history) {
            if (!sensor.equals(current.sensorId())) {
                continue;
            }

            if (detector.evaluate(current).isPresent()) {
                generatedAlerts++;
            }
        }

        assertEquals(1, generatedAlerts);

        assertFalse(inbox.findPending(1000)
                .contains(originals.get(0)));

        assertTrue(inbox.findPending(1000)
                .contains(originals.get(4)));
    }

}
