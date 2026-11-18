package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.BruteForceDetector;
import dev.kayquemiguel.aegis.detection.persistence.AuthenticationAlertCommitService;
import dev.kayquemiguel.aegis.detection.persistence.AuthenticationEventInbox;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DurableAuthenticationProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DurableAuthenticationProcessor.class
            );


    private static final int PAGE_SIZE = 500;
    private static final int RECOVERY_BATCH_SIZE = 500;

    // Constant, application-wide PostgreSQL advisory lock.
    private static final long PROCESSING_LOCK = 4101003L;

    private final AuthenticationEventInbox inbox;
    private final AuthenticationAlertCommitService commits;
    private final JdbcTemplate jdbc;

    public DurableAuthenticationProcessor(
            AuthenticationEventInbox inbox,
            AuthenticationAlertCommitService commits,
            JdbcTemplate jdbc
    ) {
        this.inbox = Objects.requireNonNull(inbox);
        this.commits = Objects.requireNonNull(commits);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional(timeout = 120)
    public Optional<UUID> process(AuthenticationEvent incoming) {
        Objects.requireNonNull(incoming, "event required");

        // Transaction-scoped across application instances.
        acquireProcessingLock();

        // Duplicates are validated against their original payload.
        inbox.record(incoming);

        replayAndAcknowledge(earliestPending(), Integer.MAX_VALUE);

        List<UUID> ids = jdbc.query(
                """
                SELECT alert_id
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND event_id = ?
                  AND processed = TRUE
                  AND alert_id IS NOT NULL
                """,
                (rs, rowNum) ->
                        rs.getObject("alert_id", UUID.class),
                incoming.sensorId(),
                incoming.id()
        );

        return ids.stream().findFirst();
    }

    @Transactional(timeout = 120)
    public int recoverPending() {
        acquireProcessingLock();

        return replayAndAcknowledge(
                earliestPending(),
                RECOVERY_BATCH_SIZE
        );
    }

    private void acquireProcessingLock() {
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(?)",
                Object.class,
                PROCESSING_LOCK
        );
    }

    private Instant earliestPending() {
        java.sql.Timestamp timestamp = jdbc.queryForObject(
                "SELECT MIN(ingested_at) "
                + "FROM auth_event_inbox "
                + "WHERE processed = FALSE",
                java.sql.Timestamp.class
        );

        return timestamp == null ? null : timestamp.toInstant();
    }

    private int replayAndAcknowledge(
            Instant earliestPending,
            int maxAcknowledged
    ) {
        if (earliestPending == null) {
            return 0;
        }

        long startedAt = System.nanoTime();
        int pagesScanned = 0;
        long eventsScanned = 0;
        int eventsAcknowledged = 0;

        Instant lowerBound = earliestPending.minus(
                Duration.ofMinutes(10)
        );
        BruteForceDetector detector = new BruteForceDetector(
                Duration.ofSeconds(60), 5
        );

        Instant cursorTime = null;
        String cursorSensor = null;
        UUID cursorEvent = null;

        while (true) {
            List<AuthenticationEventInbox.ReplayEntry> page =
                    inbox.findReplayEntriesSince(
                            lowerBound,
                            cursorTime,
                            cursorSensor,
                            cursorEvent,
                            PAGE_SIZE
                    );

            if (page.isEmpty()) {
                break;
            }

            pagesScanned++;
            eventsScanned += page.size();

            for (var entry : page) {
                AuthenticationEvent event = entry.event();
                var detection = detector.evaluate(event);

                if (!entry.processed()) {
                    if (detection.isPresent()) {
                        commits.persistAndAcknowledge(
                                event,
                                detection.orElseThrow()
                        );
                    } else {
                        commits.acknowledgeWithoutAlert(event);
                    }

                    eventsAcknowledged++;
                }

                cursorTime = event.ingestedAt();
                cursorSensor = event.sensorId();
                cursorEvent = event.id();

                if (eventsAcknowledged >= maxAcknowledged) {
                    break;
                }
            }

            if (eventsAcknowledged >= maxAcknowledged
                    || page.size() < PAGE_SIZE) {
                break;
            }
        }

        long elapsedMillis =
                java.util.concurrent.TimeUnit.NANOSECONDS
                        .toMillis(System.nanoTime() - startedAt);

        log.info(
                "AEGIS-101 replay examined: pages={}, "
                + "events={}, acknowledged={}, elapsedMs={}",
                pagesScanned,
                eventsScanned,
                eventsAcknowledged,
                elapsedMillis
        );

        return eventsAcknowledged;
    }

}
