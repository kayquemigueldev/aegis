
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SuricataImportRunnerTest {

    @Autowired
    private SuricataDetectionPipeline pipeline;

    @Autowired
    private DetectionAlertRepository repository;

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
                """.formatted(
                second,
                port,
                flowId
        ).replace("\n", "");
    }

    private Path createFile(String... lines)
            throws IOException {
        Path path = tempDir.resolve(
                UUID.randomUUID() + ".jsonl"
        );

        Files.write(path, List.of(lines));
        return path;
    }

    private SuricataImportRunner runner(
            String file,
            String sensorId
    ) {
        return new SuricataImportRunner(
                pipeline,
                file,
                sensorId
        );
    }

    private static DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(
                new String[0]
        );
    }

    @Test
    void shouldImportAndPersistAlertFromCli()
            throws Exception {

        String sensorId = sensor();

        Path path = createFile(
                flow(22, 1001, 0),
                flow(80, 1002, 10),
                flow(443, 1003, 20)
        );

        runner(path.toString(), sensorId)
                .run(arguments());

        repository.flush();

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());
        assertEquals("NET-001", alerts.getFirst().getRuleId());
        assertEquals(sensorId, alerts.getFirst().getSensorId());
        assertEquals(3, alerts.getFirst().getDistinctPorts());
        assertEquals(3, alerts.getFirst().getDetectionThreshold());
    }

    @Test
    void shouldNotCreateAlertBelowThreshold()
            throws Exception {

        String sensorId = sensor();

        Path path = createFile(
                flow(22, 2001, 0),
                flow(80, 2002, 10)
        );

        runner(path.toString(), sensorId)
                .run(arguments());

        repository.flush();

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldIgnoreMalformedLinesAndContinue()
            throws Exception {

        String sensorId = sensor();

        Path path = createFile(
                "{invalid}",
                flow(22, 3001, 0),
                flow(80, 3002, 10),
                flow(443, 3003, 20)
        );

        runner(path.toString(), sensorId)
                .run(arguments());

        repository.flush();

        assertEquals(
                1,
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).size()
        );
    }

    @Test
    void shouldDoNothingWithoutImportArguments()
            throws Exception {

        assertDoesNotThrow(
                () -> runner("", "").run(arguments())
        );
    }

    @Test
    void shouldRejectMissingSensor()
            throws IOException {

        Path path = createFile(
                flow(22, 4001, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> runner(
                        path.toString(), ""
                ).run(arguments())
        );
    }

    @Test
    void shouldRejectMissingFile()
            throws Exception {

        String sensorId = sensor();

        Path nonexistent = tempDir.resolve(
                "does-not-exist.jsonl"
        );

        assertThrows(
                IOException.class,
                () -> runner(
                        nonexistent.toString(),
                        sensorId
                ).run(arguments())
        );

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldRejectSensorWithoutFile()
            throws Exception {

        assertThrows(
                IllegalArgumentException.class,
                () -> runner(
                        "", sensor()
                ).run(arguments())
        );
    }
}
