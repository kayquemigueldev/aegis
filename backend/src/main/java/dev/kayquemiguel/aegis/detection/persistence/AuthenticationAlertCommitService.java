package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthenticationAlertCommitService {

    private final JdbcTemplate jdbc;
    private final AuthenticationEventInbox inbox;

    public AuthenticationAlertCommitService(
            JdbcTemplate jdbc,
            AuthenticationEventInbox inbox
    ) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.inbox = Objects.requireNonNull(inbox);
    }

    @Transactional
    public UUID persistAndAcknowledge(
            AuthenticationEvent event,
            AuthenticationDetectionAlert alert
    ) {
        Objects.requireNonNull(event, "event required");
        Objects.requireNonNull(alert, "alert required");

        if (!"NET-003".equals(alert.ruleId())
                || !event.isFailure()
                || !event.sensorId().equals(alert.sensorId())
                || !event.sourceSystem().equals(alert.sourceSystem())
                || !event.sourceIp().equals(alert.sourceIp())
                || !event.username().equals(alert.username())
                || alert.observedCount() < 1
                || alert.threshold() < 1) {
            throw new IllegalArgumentException(
                    "Alert does not match trigger event"
            );
        }

        // Serializes retries for one inbox event.
        List<InboxState> states = jdbc.query(
                """
                SELECT processed, alert_id,
                       occurred_at, source_ip, username,
                       source_system, outcome
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND event_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new InboxState(
                        rs.getBoolean("processed"),
                        rs.getObject("alert_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("source_ip"),
                        rs.getString("username"),
                        rs.getString("source_system"),
                        rs.getString("outcome")
                ),
                event.sensorId(),
                event.id()
        );

        if (states.isEmpty()) {
            throw new IllegalStateException(
                    "Trigger event is not in durable inbox"
            );
        }

        InboxState state = states.get(0);

        if (!state.occurredAt().equals(event.occurredAt())
                || !state.sourceIp().equals(event.sourceIp())
                || !state.username().equals(event.username())
                || !state.sourceSystem().equals(event.sourceSystem())
                || !state.outcome().equals(event.outcome().name())) {
            throw new IllegalStateException(
                    "Trigger event payload differs from inbox"
            );
        }

        if (state.processed()) {
            if (state.alertId() == null) {
                throw new IllegalStateException(
                        "Event already processed without an alert"
                );
            }
            return state.alertId();
        }

        UUID candidateId = UUID.randomUUID();

        List<UUID> inserted = jdbc.query(
                """
                INSERT INTO detection_alerts (
                    id, rule_id, sensor_id,
                    source_ip, destination_ip,
                    detected_at, distinct_ports,
                    metric_type, observed_count,
                    detection_threshold, description,
                    auth_username, auth_source_system,
                    trigger_event_id
                )
                VALUES (
                    ?, 'NET-003', ?, ?, NULL,
                    ?, 0, 'AUTHENTICATION_FAILURES',
                    ?, ?, 'Repeated authentication failures',
                    ?, ?, ?
                )
                ON CONFLICT (sensor_id, trigger_event_id)
                    WHERE rule_id = 'NET-003'
                      AND trigger_event_id IS NOT NULL
                DO NOTHING
                RETURNING id
                """,
                (rs, rowNum) ->
                        rs.getObject("id", UUID.class),
                candidateId,
                alert.sensorId(),
                alert.sourceIp(),
                Timestamp.from(alert.detectedAt()),
                alert.observedCount(),
                alert.threshold(),
                alert.username(),
                alert.sourceSystem(),
                event.id()
        );

        UUID alertId;

        if (!inserted.isEmpty()) {
            alertId = inserted.get(0);
        } else {
            alertId = jdbc.queryForObject(
                    """
                    SELECT id
                    FROM detection_alerts
                    WHERE rule_id = 'NET-003'
                      AND sensor_id = ?
                      AND trigger_event_id = ?
                    """,
                    UUID.class,
                    event.sensorId(),
                    event.id()
            );
        }

        if (alertId == null || !inbox.markProcessed(
                event.sensorId(),
                event.id(),
                alertId
        )) {
            throw new IllegalStateException(
                    "Unable to acknowledge trigger event"
            );
        }

        return alertId;
    }


    @Transactional
    public boolean acknowledgeWithoutAlert(
            AuthenticationEvent event
    ) {
        Objects.requireNonNull(event, "event required");

        List<InboxState> states = jdbc.query(
                """
                SELECT processed, alert_id,
                       occurred_at, source_ip, username,
                       source_system, outcome
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND event_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> new InboxState(
                        rs.getBoolean("processed"),
                        rs.getObject("alert_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("source_ip"),
                        rs.getString("username"),
                        rs.getString("source_system"),
                        rs.getString("outcome")
                ),
                event.sensorId(),
                event.id()
        );

        if (states.isEmpty()) {
            throw new IllegalStateException(
                    "Event is not in durable inbox"
            );
        }

        InboxState state = states.get(0);

        if (!state.occurredAt().equals(event.occurredAt())
                || !state.sourceIp().equals(event.sourceIp())
                || !state.username().equals(event.username())
                || !state.sourceSystem().equals(event.sourceSystem())
                || !state.outcome().equals(event.outcome().name())) {
            throw new IllegalStateException(
                    "Event payload differs from inbox"
            );
        }

        if (state.processed()) {
            if (state.alertId() != null) {
                throw new IllegalStateException(
                        "Event already generated an alert"
                );
            }

            return false;
        }

        return inbox.markProcessed(
                event.sensorId(),
                event.id(),
                null
        );
    }

    private record InboxState(
            boolean processed,
            UUID alertId,
            java.time.Instant occurredAt,
            String sourceIp,
            String username,
            String sourceSystem,
            String outcome
    ) {
    }
}
