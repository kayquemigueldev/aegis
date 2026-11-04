
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class ImportRunRepository {

    private final JdbcTemplate jdbc;

    public ImportRunRepository(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    public boolean claim(
            String sensorId,
            String fileSha256
    ) {
        int inserted = jdbc.update(
                """
                INSERT INTO import_runs (
                    id, sensor_id, file_sha256
                )
                VALUES (?, ?, ?)
                ON CONFLICT (sensor_id, file_sha256)
                DO NOTHING
                """,
                UUID.randomUUID(),
                sensorId,
                fileSha256
        );

        return inserted == 1;
    }

    public void complete(
            String sensorId,
            String fileSha256,
            ImportReport report,
            long persistedAlerts
    ) {
        int updated = jdbc.update(
                """
                UPDATE import_runs SET
                    total_lines = ?,
                    accepted_events = ?,
                    ignored_events = ?,
                    rejected_events = ?,
                    persisted_alerts = ?,
                    completed_at = CURRENT_TIMESTAMP
                WHERE sensor_id = ?
                  AND file_sha256 = ?
                  AND completed_at IS NULL
                """,
                report.totalLines(),
                report.accepted(),
                report.ignored(),
                report.rejected(),
                persistedAlerts,
                sensorId,
                fileSha256
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Could not complete import run"
            );
        }
    }

    public ImportReport previousReport(
            String sensorId,
            String fileSha256
    ) {
        return jdbc.query(
                """
                SELECT total_lines,
                       accepted_events,
                       ignored_events,
                       rejected_events
                FROM import_runs
                WHERE sensor_id = ?
                  AND file_sha256 = ?
                  AND completed_at IS NOT NULL
                """,
                rs -> {
                    if (!rs.next()) {
                        throw new IllegalStateException(
                                "Completed import record not found"
                        );
                    }

                    return new ImportReport(
                            rs.getLong("total_lines"),
                            rs.getLong("accepted_events"),
                            rs.getLong("ignored_events"),
                            rs.getLong("rejected_events"),
                            List.of()
                    );
                },
                sensorId,
                fileSha256
        );
    }
}
