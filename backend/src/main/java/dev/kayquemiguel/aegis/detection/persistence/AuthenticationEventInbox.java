package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import java.util.Objects;

@Repository
public class AuthenticationEventInbox {

    private final JdbcTemplate jdbc;

    public AuthenticationEventInbox(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional
    public boolean record(AuthenticationEvent event) {
        Objects.requireNonNull(
                event, "event is required"
        );

        int inserted = jdbc.update(
                """
                INSERT INTO auth_event_inbox (
                    sensor_id,
                    event_id,
                    occurred_at,
                    ingested_at,
                    source_ip,
                    username,
                    source_system,
                    outcome
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (sensor_id, event_id)
                DO NOTHING
                """,
                event.sensorId(),
                event.id(),
                Timestamp.from(event.occurredAt()),
                Timestamp.from(event.ingestedAt()),
                event.sourceIp(),
                event.username(),
                event.sourceSystem(),
                event.outcome().name()
        );

        if (inserted == 1) {
            return true;
        }

        var existing = jdbc.queryForMap(
                """
                SELECT occurred_at,
                       source_ip,
                       username,
                       source_system,
                       outcome
                FROM auth_event_inbox
                WHERE sensor_id = ?
                  AND event_id = ?
                """,
                event.sensorId(),
                event.id()
        );

        boolean samePayload =
                ((Timestamp) existing.get("occurred_at"))
                        .toInstant().equals(event.occurredAt())
                && event.sourceIp().equals(
                        existing.get("source_ip"))
                && event.username().equals(
                        existing.get("username"))
                && event.sourceSystem().equals(
                        existing.get("source_system"))
                && event.outcome().name().equals(
                        existing.get("outcome"));

        if (!samePayload) {
            throw new AuthenticationEventConflictException(
                    "Conflicting authentication event ID"
            );
        }

        return false;
    }

    public List<AuthenticationEvent> findPending(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new AuthenticationEventLimitException(
                    "limit must be between 1 and 1000"
            );
        }

        return jdbc.query(
                """
                SELECT sensor_id,
                       event_id,
                       occurred_at,
                       ingested_at,
                       source_ip,
                       username,
                       source_system,
                       outcome
                FROM auth_event_inbox
                WHERE processed = FALSE
                ORDER BY ingested_at, sensor_id, event_id
                LIMIT ?
                """,
                (rs, rowNum) -> new AuthenticationEvent(
                        rs.getObject("event_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getTimestamp("ingested_at").toInstant(),
                        rs.getString("source_ip"),
                        rs.getString("username"),
                        rs.getString("source_system"),
                        rs.getString("sensor_id"),
                        AuthenticationOutcome.valueOf(
                                rs.getString("outcome")
                        )
                ),
                limit
        );
    }

    @Transactional
    public boolean markProcessed(
            String sensorId,
            UUID eventId,
            UUID alertId
    ) {
        Objects.requireNonNull(sensorId, "sensorId required");
        Objects.requireNonNull(eventId, "eventId required");

        int updated = jdbc.update(
                """
                UPDATE auth_event_inbox
                SET processed = TRUE,
                    alert_id = ?
                WHERE sensor_id = ?
                  AND event_id = ?
                  AND processed = FALSE
                """,
                alertId,
                sensorId,
                eventId
        );

        return updated == 1;
    }


    public List<AuthenticationEvent> findReplayPage(
            java.time.Instant afterIngestedAt,
            String afterSensorId,
            UUID afterEventId,
            int limit
    ) {
        if (limit < 1 || limit > 1000) {
            throw new AuthenticationEventLimitException(
                    "limit must be between 1 and 1000"
            );
        }

        boolean initial = afterIngestedAt == null
                && afterSensorId == null
                && afterEventId == null;

        boolean completeCursor = afterIngestedAt != null
                && afterSensorId != null
                && afterEventId != null;

        if (!initial && !completeCursor) {
            throw new AuthenticationEventLimitException(
                    "Replay cursor must be complete"
            );
        }

        String columns = """
                SELECT sensor_id, event_id,
                       occurred_at, ingested_at,
                       source_ip, username,
                       source_system, outcome
                FROM auth_event_inbox
                """;

        String order = """
                ORDER BY ingested_at, sensor_id, event_id
                LIMIT ?
                """;

        String sql = initial
                ? columns + order
                : columns + """
                  WHERE (ingested_at, sensor_id, event_id)
                        > (?, ?, ?)
                  """ + order;

        Object[] parameters = initial
                ? new Object[] { limit }
                : new Object[] {
                        Timestamp.from(afterIngestedAt),
                        afterSensorId,
                        afterEventId,
                        limit
                };

        return jdbc.query(
                sql,
                (rs, rowNum) -> new AuthenticationEvent(
                        rs.getObject("event_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getTimestamp("ingested_at").toInstant(),
                        rs.getString("source_ip"),
                        rs.getString("username"),
                        rs.getString("source_system"),
                        rs.getString("sensor_id"),
                        AuthenticationOutcome.valueOf(
                                rs.getString("outcome")
                        )
                ),
                parameters
        );
    }


    public List<AuthenticationEvent> findReplayPageSince(
            java.time.Instant fromInclusive,
            java.time.Instant afterIngestedAt,
            String afterSensorId,
            UUID afterEventId,
            int limit
    ) {
        Objects.requireNonNull(
                fromInclusive, "Replay lower bound required"
        );

        if (limit < 1 || limit > 1000) {
            throw new AuthenticationEventLimitException(
                    "limit must be between 1 and 1000"
            );
        }

        boolean initial = afterIngestedAt == null
                && afterSensorId == null
                && afterEventId == null;

        boolean complete = afterIngestedAt != null
                && afterSensorId != null
                && afterEventId != null;

        if (!initial && !complete) {
            throw new AuthenticationEventLimitException(
                    "Replay cursor must be complete"
            );
        }

        String sql = """
                SELECT sensor_id, event_id,
                       occurred_at, ingested_at,
                       source_ip, username,
                       source_system, outcome
                FROM auth_event_inbox
                WHERE ingested_at >= ?
                """;

        Object[] parameters;

        if (initial) {
            parameters = new Object[] {
                    Timestamp.from(fromInclusive),
                    limit
            };
        } else {
            sql += """
                    AND (ingested_at, sensor_id, event_id)
                        > (?, ?, ?)
                    """;

            parameters = new Object[] {
                    Timestamp.from(fromInclusive),
                    Timestamp.from(afterIngestedAt),
                    afterSensorId,
                    afterEventId,
                    limit
            };
        }

        sql += """
                ORDER BY ingested_at, sensor_id, event_id
                LIMIT ?
                """;

        return jdbc.query(
                sql,
                (rs, rowNum) -> new AuthenticationEvent(
                        rs.getObject("event_id", UUID.class),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getTimestamp("ingested_at").toInstant(),
                        rs.getString("source_ip"),
                        rs.getString("username"),
                        rs.getString("source_system"),
                        rs.getString("sensor_id"),
                        AuthenticationOutcome.valueOf(
                                rs.getString("outcome")
                        )
                ),
                parameters
        );
    }

}
