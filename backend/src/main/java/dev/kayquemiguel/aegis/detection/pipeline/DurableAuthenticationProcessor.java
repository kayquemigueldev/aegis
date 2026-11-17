package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.BruteForceDetector;
import dev.kayquemiguel.aegis.detection.persistence.AuthenticationAlertCommitService;
import dev.kayquemiguel.aegis.detection.persistence.AuthenticationEventInbox;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
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

    private static final int PAGE_SIZE = 500;

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

    @Transactional
    public Optional<UUID> process(AuthenticationEvent incoming) {
        Objects.requireNonNull(incoming, "event required");

        // Transaction-scoped across application instances.
        acquireProcessingLock();

        // Duplicates are validated against their original payload.
        inbox.record(incoming);

        replayAndAcknowledge();

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

    @Transactional
    public int recoverPending() {
        acquireProcessingLock();

        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM auth_event_inbox "
                + "WHERE processed = FALSE",
                Integer.class
        );

        int count = Objects.requireNonNull(pending);

        if (count == 0) {
            return 0;
        }

        replayAndAcknowledge();

        return count;
    }

    private void acquireProcessingLock() {
        jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(?)",
                Object.class,
                PROCESSING_LOCK
        );
    }

    private void replayAndAcknowledge() {
        BruteForceDetector detector = new BruteForceDetector(
                Duration.ofSeconds(60), 5
        );

        Instant cursorTime = null;
        String cursorSensor = null;
        UUID cursorEvent = null;

        while (true) {
            List<AuthenticationEvent> page =
                    inbox.findReplayPage(
                            cursorTime,
                            cursorSensor,
                            cursorEvent,
                            PAGE_SIZE
                    );

            if (page.isEmpty()) {
                break;
            }

            for (AuthenticationEvent event : page) {
                var detection = detector.evaluate(event);

                Boolean processed = jdbc.queryForObject(
                        """
                        SELECT processed
                        FROM auth_event_inbox
                        WHERE sensor_id = ?
                          AND event_id = ?
                        """,
                        Boolean.class,
                        event.sensorId(),
                        event.id()
                );

                if (Boolean.FALSE.equals(processed)) {
                    if (detection.isPresent()) {
                        commits.persistAndAcknowledge(
                                event,
                                detection.orElseThrow()
                        );
                    } else {
                        commits.acknowledgeWithoutAlert(event);
                    }
                }

                cursorTime = event.ingestedAt();
                cursorSensor = event.sensorId();
                cursorEvent = event.id();
            }

            if (page.size() < PAGE_SIZE) {
                break;
            }
        }

    }

}
