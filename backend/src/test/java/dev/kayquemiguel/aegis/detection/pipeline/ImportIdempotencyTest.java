
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ImportIdempotencyTest {

    @Autowired
    private SuricataDetectionPipeline pipeline;

    @Autowired
    private DetectionAlertRepository alerts;

    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path tempDir;

    private static String sensor() {
        return "test-" + UUID.randomUUID();
    }

    private static String flow(
            int port,
            int flowId,
            int second
    ) {
        return """
                {"timestamp":"2026-11-03T10:00:%02dZ",
                "event_type":"flow",
                "src_ip":"192.0.2.10",
                "dest_ip":"192.0.2.20",
                "src_port":49152,
                "dest_port":%d,
                "proto":"TCP",
                "flow_id":%d}
                """.formatted(second, port, flowId)
                .replace("\n", "");
    }

    private Path createFile(String... lines)
            throws IOException {
        Path file = tempDir.resolve(
                UUID.randomUUID() + ".jsonl"
        );

        Files.write(file, List.of(lines));

        return file;
    }

    private Path standardFile() throws IOException {
        return createFile(
                flow(22, 1001, 0),
                flow(80, 1002, 10),
                flow(443, 1003, 20)
        );
    }

    private long alertCount(String sensorId) {
        return alerts
                .findBySensorIdOrderByDetectedAtDesc(sensorId)
                .size();
    }

    private long runCount(String sensorId) {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM import_runs
                WHERE sensor_id = ?
                """,
                Long.class,
                sensorId
        );

        return count == null ? 0 : count;
    }

    @Test
    void shouldSkipSameFileOnSecondImport()
            throws IOException {
        String sensorId = sensor();
        Path path = standardFile();

        DetectionPipelineReport first =
                pipeline.process(path, sensorId);

        DetectionPipelineReport second =
                pipeline.process(path, sensorId);

        assertFalse(first.alreadyImported());
        assertEquals(1, first.alertsPersisted());

        assertTrue(second.alreadyImported());
        assertEquals(0, second.alertsPersisted());
        assertEquals(3, second.importReport().accepted());

        assertEquals(1, alertCount(sensorId));
        assertEquals(1, runCount(sensorId));
    }

    @Test
    void shouldDetectDuplicateAfterRenamingFile()
            throws IOException {
        String sensorId = sensor();
        Path original = standardFile();

        Path renamed = tempDir.resolve("renamed.jsonl");

        Files.copy(original, renamed);

        pipeline.process(original, sensorId);

        DetectionPipelineReport repeated =
                pipeline.process(renamed, sensorId);

        assertTrue(repeated.alreadyImported());
        assertEquals(0, repeated.alertsPersisted());
        assertEquals(1, alertCount(sensorId));
        assertEquals(1, runCount(sensorId));
    }

    @Test
    void shouldImportChangedContent()
            throws IOException {
        String sensorId = sensor();
        Path path = standardFile();

        DetectionPipelineReport first =
                pipeline.process(path, sensorId);

        // Novo conteudo, novo hash.
        Files.write(path, List.of(
                flow(8080, 2001, 0),
                flow(8443, 2002, 10),
                flow(9090, 2003, 20)
        ));

        DetectionPipelineReport second =
                pipeline.process(path, sensorId);

        assertFalse(first.alreadyImported());
        assertFalse(second.alreadyImported());

        assertEquals(1, first.alertsPersisted());
        assertEquals(1, second.alertsPersisted());

        assertEquals(2, alertCount(sensorId));
        assertEquals(2, runCount(sensorId));
    }

    @Test
    void shouldAllowSameFileForDifferentSensors()
            throws IOException {
        Path path = standardFile();

        String sensorA = sensor();
        String sensorB = sensor();

        DetectionPipelineReport first =
                pipeline.process(path, sensorA);

        DetectionPipelineReport second =
                pipeline.process(path, sensorB);

        assertFalse(first.alreadyImported());
        assertFalse(second.alreadyImported());

        assertEquals(1, alertCount(sensorA));
        assertEquals(1, alertCount(sensorB));

        assertEquals(1, runCount(sensorA));
        assertEquals(1, runCount(sensorB));
    }

    @Test
    void shouldNotRegisterMissingFile()
            throws IOException {
        String sensorId = sensor();

        Path missing = tempDir.resolve(
                "missing-file.jsonl"
        );

        assertThrows(
                IOException.class,
                () -> pipeline.process(missing, sensorId)
        );

        assertEquals(0, runCount(sensorId));
        assertEquals(0, alertCount(sensorId));
    }
}
