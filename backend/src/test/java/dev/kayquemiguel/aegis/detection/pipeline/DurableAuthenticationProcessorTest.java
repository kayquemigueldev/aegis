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
import java.util.HashSet;
import java.util.Set;
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

    @Test
    void shouldRecoverPendingAlertWithoutNewRequest() {
        String sensorId = sensor();

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(
                        jdbc
                );

        for (int i = 0; i < 5; i++) {
            AuthenticationEvent current =
                    failure(sensorId, i);

            assertTrue(inbox.record(current));

            if (i < 4) {
                assertTrue(inbox.markProcessed(
                        sensorId,
                        current.id(),
                        null
                ));
            }
        }

        assertEquals(1, processor.recoverPending());
        assertEquals(0, processor.recoverPending());

        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND processed = FALSE",
                Integer.class,
                sensorId
        );

        Integer alerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? AND rule_id = 'NET-003'",
                Integer.class,
                sensorId
        );

        assertEquals(0, pending);
        assertEquals(1, alerts);
    }

    @Test
    void shouldRecoverPendingEventWithoutAlert() {
        String sensorId = sensor();

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(
                        jdbc
                );

        AuthenticationEvent original =
                failure(sensorId, 0);

        assertTrue(inbox.record(original));

        assertEquals(1, processor.recoverPending());

        Boolean processed = jdbc.queryForObject(
                "SELECT processed FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                Boolean.class,
                sensorId,
                original.id()
        );

        Integer alerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? AND rule_id = 'NET-003'",
                Integer.class,
                sensorId
        );

        assertEquals(Boolean.TRUE, processed);
        assertEquals(0, alerts);
    }


    @Test
    void shouldExcludeExpiredHistoryFromReplayPage() {
        String sensorId = sensor();

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        AuthenticationEvent old = failure(sensorId, 0);
        AuthenticationEvent recent = failure(sensorId, 720);

        assertTrue(inbox.record(old));
        assertTrue(inbox.record(recent));

        var page = inbox.findReplayPageSince(
                recent.ingestedAt().minusSeconds(600),
                null,
                null,
                null,
                1000
        );

        assertTrue(page.contains(recent));
        assertFalse(page.contains(old));
    }

    @Test
    void shouldPreserveDetectionForOlderPendingEvent() {
        String sensorId = sensor();

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        AuthenticationEvent fifth = null;

        for (int i = 0; i < 5; i++) {
            AuthenticationEvent current = failure(sensorId, i);

            assertTrue(inbox.record(current));

            if (i < 4) {
                assertTrue(inbox.markProcessed(
                        current.sensorId(),
                        current.id(),
                        null
                ));
            } else {
                fifth = current;
            }
        }

        // A newer processed event must not move the replay
        // lower bound past the older pending fifth failure.
        AuthenticationEvent newer = failure(sensorId, 720);

        assertTrue(inbox.record(newer));
        assertTrue(inbox.markProcessed(
                newer.sensorId(),
                newer.id(),
                null
        ));

        assertEquals(1, processor.recoverPending());

        Integer alerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                + "WHERE sensor_id = ? "
                + "AND rule_id = 'NET-003'",
                Integer.class,
                sensorId
        );

        assertEquals(1, alerts);

        UUID linked = jdbc.queryForObject(
                "SELECT alert_id FROM auth_event_inbox "
                + "WHERE sensor_id = ? AND event_id = ?",
                UUID.class,
                sensorId,
                fifth.id()
        );

        assertNotNull(linked);
    }


    @Test
    void shouldReturnProcessedStateWithReplayEntries() {
        String sensorId = sensor();

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        AuthenticationEvent acknowledged = failure(sensorId, 0);
        AuthenticationEvent pending = failure(sensorId, 1);

        assertTrue(inbox.record(acknowledged));
        assertTrue(inbox.record(pending));

        assertTrue(inbox.markProcessed(
                sensorId,
                acknowledged.id(),
                null
        ));

        var entries = inbox.findReplayEntriesSince(
                acknowledged.ingestedAt().minusSeconds(1),
                null,
                null,
                null,
                1000
        ).stream()
                .filter(entry ->
                        entry.event().sensorId().equals(sensorId))
                .toList();

        assertEquals(2, entries.size());

        assertEquals(
                acknowledged.id(),
                entries.get(0).event().id()
        );
        assertTrue(entries.get(0).processed());

        assertEquals(
                pending.id(),
                entries.get(1).event().id()
        );
        assertFalse(entries.get(1).processed());
    }


    @Test
    void shouldPaginateEqualTimestampsWithoutSkippingEvents() {
        String sensorId = sensor();
        Instant timestamp =
                Instant.parse("2045-01-01T10:00:00Z");

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        Set<UUID> expected = new HashSet<>();

        for (int i = 0; i < 505; i++) {
            AuthenticationEvent event = new AuthenticationEvent(
                    UUID.randomUUID(),
                    timestamp,
                    timestamp,
                    "192.0.2.20",
                    "pagination-user",
                    "pagination-test",
                    sensorId,
                    AuthenticationOutcome.SUCCESS
            );

            assertTrue(inbox.record(event));
            assertTrue(expected.add(event.id()));
        }

        Set<UUID> collected = new HashSet<>();
        Instant cursorTime = null;
        String cursorSensor = null;
        UUID cursorEvent = null;

        int pages = 0;

        while (true) {
            var page = inbox.findReplayEntriesSince(
                    timestamp,
                    cursorTime,
                    cursorSensor,
                    cursorEvent,
                    500
            );

            if (page.isEmpty()) {
                break;
            }

            pages++;

            for (var entry : page) {
                var event = entry.event();

                if (sensorId.equals(event.sensorId())) {
                    assertTrue(
                            collected.add(event.id()),
                            "Duplicate event across replay pages"
                    );
                }

                cursorTime = event.ingestedAt();
                cursorSensor = event.sensorId();
                cursorEvent = event.id();
            }

            if (page.size() < 500) {
                break;
            }
        }

        assertTrue(pages >= 2);
        assertEquals(expected, collected);
        assertEquals(505, collected.size());
    }

    @Test
    void shouldDetectBruteForceAcrossReplayPageBoundary() {
        String sensorId = sensor();
        Instant base =
                Instant.parse("2045-02-01T10:00:00Z");

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        // 498 successful events occupy almost all
        // of the first replay page.
        for (int i = 0; i < 498; i++) {
            AuthenticationEvent success = new AuthenticationEvent(
                    UUID.randomUUID(),
                    base,
                    base,
                    "192.0.2.21",
                    "boundary-user",
                    "pagination-test",
                    sensorId,
                    AuthenticationOutcome.SUCCESS
            );

            assertTrue(inbox.record(success));
            assertTrue(inbox.markProcessed(
                    sensorId,
                    success.id(),
                    null
            ));
        }

        AuthenticationEvent trigger = null;

        // First two failures complete page one.
        // The remaining three are on page two.
        for (int i = 0; i < 5; i++) {
            AuthenticationEvent failure = new AuthenticationEvent(
                    UUID.randomUUID(),
                    base.plusSeconds(i + 1),
                    base.plusSeconds(i + 1),
                    "192.0.2.21",
                    "boundary-user",
                    "pagination-test",
                    sensorId,
                    AuthenticationOutcome.FAILURE
            );

            assertTrue(inbox.record(failure));

            if (i < 4) {
                assertTrue(inbox.markProcessed(
                        sensorId,
                        failure.id(),
                        null
                ));
            } else {
                trigger = failure;
            }
        }

        assertNotNull(trigger);

        assertEquals(1, processor.recoverPending());
        assertEquals(0, processor.recoverPending());

        Integer processed = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND processed = TRUE
                """,
                Integer.class,
                sensorId
        );

        Integer alerts = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM detection_alerts
                WHERE sensor_id = ?
                  AND rule_id = 'NET-003'
                """,
                Integer.class,
                sensorId
        );

        UUID alertId = jdbc.queryForObject(
                """
                SELECT alert_id
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND event_id = ?
                """,
                UUID.class,
                sensorId,
                trigger.id()
        );

        assertEquals(503, processed);
        assertEquals(1, alerts);
        assertNotNull(alertId);
    }


    @Test
    void shouldConfigureTransactionTimeouts() throws Exception {
        var processing = DurableAuthenticationProcessor.class
                .getMethod(
                        "process",
                        AuthenticationEvent.class
                )
                .getAnnotation(
                        org.springframework.transaction.annotation
                                .Transactional.class
                );

        var recovery = DurableAuthenticationProcessor.class
                .getMethod("recoverPending")
                .getAnnotation(
                        org.springframework.transaction.annotation
                                .Transactional.class
                );

        assertNotNull(processing);
        assertNotNull(recovery);
        assertEquals(120, processing.timeout());
        assertEquals(120, recovery.timeout());
    }


    @Test
    void shouldRecoverBacklogInSeparateTransactions() {
        String sensorId = sensor();
        Instant base = Instant.parse("2046-01-01T10:00:00Z");

        var inbox = new dev.kayquemiguel.aegis.detection
                .persistence.AuthenticationEventInbox(jdbc);

        for (int i = 0; i < 503; i++) {
            boolean failed = i >= 498;

            AuthenticationEvent event = new AuthenticationEvent(
                    UUID.randomUUID(),
                    base.plusSeconds(failed ? i - 497 : 0),
                    base.plusSeconds(failed ? i - 497 : 0),
                    "192.0.2.55",
                    "batch-user",
                    "batch-test",
                    sensorId,
                    failed
                        ? AuthenticationOutcome.FAILURE
                        : AuthenticationOutcome.SUCCESS
            );

            assertTrue(inbox.record(event));
        }

        assertEquals(500, processor.recoverPending());

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                    + "WHERE sensor_id = ? AND processed = FALSE",
                Integer.class,
                sensorId
        );
        assertEquals(3, remaining);

        assertEquals(3, processor.recoverPending());
        assertEquals(0, processor.recoverPending());

        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                    + "WHERE sensor_id = ? AND processed = FALSE",
                Integer.class,
                sensorId
        );

        Integer alerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM detection_alerts "
                    + "WHERE sensor_id = ? AND rule_id = 'NET-003'",
                Integer.class,
                sensorId
        );

        assertEquals(0, pending);
        assertEquals(1, alerts);
    }

}
