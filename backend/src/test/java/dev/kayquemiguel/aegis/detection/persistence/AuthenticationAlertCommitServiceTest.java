package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
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
class AuthenticationAlertCommitServiceTest {

    @Autowired
    AuthenticationEventInbox inbox;

    @Autowired
    AuthenticationAlertCommitService service;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager
            applicationTransactionManager;

    private final List<String> sensors = new ArrayList<>();

    private AuthenticationEvent event() {
        String sensor = "atomic-" + UUID.randomUUID();
        sensors.add(sensor);

        Instant base =
                Instant.parse("2026-11-17T10:00:00Z");

        return new AuthenticationEvent(
                UUID.randomUUID(),
                base,
                base.plusSeconds(1),
                "192.0.2.10",
                "admin",
                "auth-log",
                sensor,
                AuthenticationOutcome.FAILURE
        );
    }

    private AuthenticationDetectionAlert alert(
            AuthenticationEvent event
    ) {
        return new AuthenticationDetectionAlert(
                "NET-003",
                event.occurredAt(),
                event.sensorId(),
                event.sourceSystem(),
                event.sourceIp(),
                event.username(),
                5,
                5
        );
    }

    @AfterEach
    void cleanup() {
        for (String sensor : sensors) {
            jdbc.update(
                    "DELETE FROM auth_event_inbox "
                    + "WHERE sensor_id = ?",
                    sensor
            );

            jdbc.update(
                    "DELETE FROM detection_alerts "
                    + "WHERE sensor_id = ? "
                    + "AND rule_id = 'NET-003'",
                    sensor
            );
        }
    }

    @Test
    void shouldSaveAlertAndAcknowledgeEventTogether() {
        AuthenticationEvent event = event();
        assertTrue(inbox.record(event));

        UUID id = service.persistAndAcknowledge(
                event, alert(event)
        );

        UUID linkedId = jdbc.queryForObject(
                "SELECT alert_id FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                UUID.class,
                event.sensorId(),
                event.id()
        );

        Boolean processed = jdbc.queryForObject(
                "SELECT processed FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Boolean.class,
                event.sensorId(),
                event.id()
        );

        assertEquals(id, linkedId);
        assertEquals(Boolean.TRUE, processed);
    }

    @Test
    void shouldReturnSameAlertOnRetry() {
        AuthenticationEvent event = event();
        assertTrue(inbox.record(event));

        UUID first = service.persistAndAcknowledge(
                event, alert(event)
        );

        UUID second = service.persistAndAcknowledge(
                event, alert(event)
        );

        assertEquals(first, second);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? "
                + "AND trigger_event_id = ?",
                Integer.class,
                event.sensorId(),
                event.id()
        );

        assertEquals(1, count);
    }

    @Test
    void shouldNotCreateAlertForMissingInboxEvent() {
        AuthenticationEvent event = event();

        assertThrows(
                IllegalStateException.class,
                () -> service.persistAndAcknowledge(
                        event, alert(event)
                )
        );

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? "
                + "AND trigger_event_id = ?",
                Integer.class,
                event.sensorId(),
                event.id()
        );

        assertEquals(0, count);
    }

    @Test
    void shouldRejectMismatchedAlertWithoutAcknowledging() {
        AuthenticationEvent event = event();
        assertTrue(inbox.record(event));

        AuthenticationDetectionAlert invalid =
                new AuthenticationDetectionAlert(
                        "NET-003",
                        event.occurredAt(),
                        event.sensorId(),
                        event.sourceSystem(),
                        event.sourceIp(),
                        "another-user",
                        5,
                        5
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.persistAndAcknowledge(
                        event, invalid
                )
        );

        assertTrue(
                inbox.findPending(1000).contains(event)
        );
    }

    @Test
    void shouldRejectModifiedTriggerEvent() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        AuthenticationEvent modified =
                new AuthenticationEvent(
                        original.id(),
                        original.occurredAt(),
                        original.ingestedAt(),
                        original.sourceIp(),
                        "different-user",
                        original.sourceSystem(),
                        original.sensorId(),
                        original.outcome()
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.persistAndAcknowledge(
                        modified,
                        alert(modified)
                )
        );

        assertEquals(
                "Trigger event payload differs from inbox",
                exception.getMessage()
        );

        assertTrue(
                inbox.findPending(1000).contains(original)
        );

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? "
                + "AND trigger_event_id = ?",
                Integer.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(0, count);
    }


    @Test
    void shouldAcknowledgeEventWithoutAlert() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        assertTrue(
                service.acknowledgeWithoutAlert(original)
        );

        assertFalse(
                inbox.findPending(1000).contains(original)
        );

        Boolean processed = jdbc.queryForObject(
                "SELECT processed FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Boolean.class,
                original.sensorId(),
                original.id()
        );

        UUID alertId = jdbc.queryForObject(
                "SELECT alert_id FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                UUID.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(Boolean.TRUE, processed);
        assertNull(alertId);
    }

    @Test
    void shouldNotAcknowledgeSameEventTwice() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        assertTrue(
                service.acknowledgeWithoutAlert(original)
        );

        assertFalse(
                service.acknowledgeWithoutAlert(original)
        );

        assertFalse(inbox.record(original));
    }

    @Test
    void shouldNotDiscardPreviouslyGeneratedAlert() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        UUID alertId = service.persistAndAcknowledge(
                original,
                alert(original)
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.acknowledgeWithoutAlert(
                                original
                        )
                );

        assertEquals(
                "Event already generated an alert",
                exception.getMessage()
        );

        UUID retained = jdbc.queryForObject(
                "SELECT alert_id FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                UUID.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(alertId, retained);
    }


    @Test
    void shouldCreateOnlyOneAlertUnderConcurrentRetries()
            throws Exception {

        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        var executor =
                java.util.concurrent.Executors
                        .newFixedThreadPool(2);

        var ready =
                new java.util.concurrent.CountDownLatch(2);

        var start =
                new java.util.concurrent.CountDownLatch(1);

        java.util.concurrent.Callable<UUID> operation = () -> {
            ready.countDown();

            if (!start.await(
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
            )) {
                throw new IllegalStateException(
                        "Concurrent start timed out"
                );
            }

            return service.persistAndAcknowledge(
                    original,
                    alert(original)
            );
        };

        try {
            var first = executor.submit(operation);
            var second = executor.submit(operation);

            assertTrue(ready.await(
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
            ));

            start.countDown();

            UUID firstId = first.get(
                    15,
                    java.util.concurrent.TimeUnit.SECONDS
            );

            UUID secondId = second.get(
                    15,
                    java.util.concurrent.TimeUnit.SECONDS
            );

            assertEquals(firstId, secondId);

            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM detection_alerts "
                    + "WHERE sensor_id = ? "
                    + "AND trigger_event_id = ?",
                    Integer.class,
                    original.sensorId(),
                    original.id()
            );

            assertEquals(1, count);

            UUID linked = jdbc.queryForObject(
                    "SELECT alert_id FROM auth_event_inbox "
                    + "WHERE sensor_id = ? AND event_id = ?",
                    UUID.class,
                    original.sensorId(),
                    original.id()
            );

            assertEquals(firstId, linked);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }


    @Test
    void shouldRollbackAlertAndAcknowledgementTogether() {
        AuthenticationEvent original = event();

        assertTrue(inbox.record(original));

        var transactionManager =
                applicationTransactionManager;

        var transaction =
                new org.springframework.transaction.support
                        .TransactionTemplate(transactionManager);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> transaction.execute(status -> {
                    service.persistAndAcknowledge(
                            original,
                            alert(original)
                    );

                    throw new IllegalStateException(
                            "Simulated failure before commit"
                    );
                })
        );

        assertEquals(
                "Simulated failure before commit",
                failure.getMessage()
        );

        Integer alertCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? "
                + "AND trigger_event_id = ?",
                Integer.class,
                original.sensorId(),
                original.id()
        );

        Boolean processed = jdbc.queryForObject(
                "SELECT processed FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Boolean.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(0, alertCount);
        assertEquals(Boolean.FALSE, processed);

        UUID recoveredId =
                service.persistAndAcknowledge(
                        original,
                        alert(original)
                );

        assertNotNull(recoveredId);

        UUID linkedId = jdbc.queryForObject(
                "SELECT alert_id FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                UUID.class,
                original.sensorId(),
                original.id()
        );

        assertEquals(recoveredId, linkedId);
    }

}
