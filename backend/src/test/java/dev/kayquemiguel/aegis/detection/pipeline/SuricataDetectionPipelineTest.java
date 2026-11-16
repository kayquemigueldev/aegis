
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SuricataDetectionPipelineTest {

    private static final Instant BASE =
            Instant.parse("2026-11-03T10:00:00Z");

    @Autowired
    private SuricataDetectionPipeline pipeline;

    @Autowired
    private DetectionAlertRepository repository;

    @TempDir
    Path tempDir;

    private String sensor() {
        return "test-" + UUID.randomUUID();
    }

    private Path file(String... lines) throws IOException {
        Path path = tempDir.resolve(
                UUID.randomUUID() + ".jsonl"
        );

        Files.write(path, List.of(lines));
        return path;
    }

    private static String flow(
            int port,
            String flowId,
            long seconds
    ) {
        return """
                {
                  "timestamp": "%s",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "dest_ip": "192.0.2.20",
                  "src_port": 49152,
                  "dest_port": %d,
                  "proto": "TCP",
                  "flow_id": %s
                }
                """.formatted(
                BASE.plusSeconds(seconds),
                port,
                flowId
        ).replace("\n", "");
    }

    @Test
    void shouldDetectAndPersistAlert()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "101", 0),
                flow(80, "102", 10),
                flow(443, "103", 20)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().totalLines());
        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.importReport().rejected());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());

        DetectionAlertEntity alert = alerts.getFirst();

        assertEquals("NET-001", alert.getRuleId());
        assertEquals(sensorId, alert.getSensorId());
        assertEquals("192.0.2.10", alert.getSourceIp());
        assertEquals("192.0.2.20", alert.getDestinationIp());
        assertEquals(3, alert.getDistinctPorts());
        assertEquals(3, alert.getDetectionThreshold());
        assertEquals(
                BASE.plusSeconds(20),
                alert.getDetectedAt()
        );
    }

    @Test
    void shouldNotPersistWithoutThreshold()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "201", 0),
                flow(80, "202", 10)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(2, report.importReport().accepted());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldReportIgnoredAndRejectedLines()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "301", 0),
                "{\"event_type\":\"alert\"}",
                "{invalid}",
                flow(80, "302", 10),
                flow(443, "303", 20)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        assertEquals(5, report.importReport().totalLines());
        assertEquals(3, report.importReport().accepted());
        assertEquals(1, report.importReport().ignored());
        assertEquals(1, report.importReport().rejected());
        assertEquals(1, report.alertsPersisted());

        assertEquals(
                3,
                report.importReport()
                        .errors().getFirst().lineNumber()
        );
    }

    @Test
    void shouldKeepSensorsSeparate()
            throws IOException {
        String sensorA = sensor();
        String sensorB = sensor();

        Path path = file(
                flow(22, "401", 0),
                flow(80, "402", 10),
                flow(443, "403", 20)
        );

        DetectionPipelineReport first =
                pipeline.process(path, sensorA);

        DetectionPipelineReport second =
                pipeline.process(path, sensorB);

        repository.flush();

        assertEquals(1, first.alertsPersisted());
        assertEquals(1, second.alertsPersisted());

        assertEquals(
                1,
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorA
                ).size()
        );

        assertEquals(
                1,
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorB
                ).size()
        );
    }

    @Test
    void shouldDeduplicateFlowWithinImport()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "501", 0),
                flow(80, "501", 10),
                flow(443, "502", 20)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldRejectInvalidSensorBeforeImport()
            throws IOException {
        Path path = file(
                flow(22, "601", 0),
                flow(80, "602", 10),
                flow(443, "603", 20)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.process(path, "invalid sensor")
        );
    }

    @Test
    void shouldNotShareDetectionStateBetweenImports()
            throws IOException {
        String sensorId = sensor();

        Path firstFile = file(
                flow(22, "701", 0),
                flow(80, "702", 10)
        );

        Path secondFile = file(
                flow(443, "703", 20)
        );

        DetectionPipelineReport first =
                pipeline.process(firstFile, sensorId);

        DetectionPipelineReport second =
                pipeline.process(secondFile, sensorId);

        repository.flush();

        assertEquals(2, first.importReport().accepted());
        assertEquals(1, second.importReport().accepted());

        assertEquals(0, first.alertsPersisted());
        assertEquals(0, second.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldDetectHorizontalScanInPipeline()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "901", 0),
                flow(22, "902", 10)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "903", 20)
                        .replace("192.0.2.20", "192.0.2.22")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());

        DetectionAlertEntity alert = alerts.getFirst();

        assertEquals("NET-002", alert.getRuleId());
        assertEquals(
                "DISTINCT_DESTINATION_HOSTS",
                alert.getMetricType()
        );
        assertEquals(3, alert.getObservedCount());
        assertEquals(sensorId, alert.getSensorId());
    }


    @Test
    void shouldDetectVerticalAndHorizontalScansTogether()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1001", 0),
                flow(80, "1002", 5),
                flow(443, "1003", 10),
                flow(22, "1004", 15)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "1005", 20)
                        .replace("192.0.2.20", "192.0.2.22")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(5, report.importReport().accepted());
        assertEquals(0, report.importReport().rejected());
        assertEquals(2, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(2, alerts.size());

        DetectionAlertEntity vertical = alerts.stream()
                .filter(a -> "NET-001".equals(a.getRuleId()))
                .findFirst()
                .orElseThrow();

        DetectionAlertEntity horizontal = alerts.stream()
                .filter(a -> "NET-002".equals(a.getRuleId()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                "DISTINCT_DESTINATION_PORTS",
                vertical.getMetricType()
        );
        assertEquals(3, vertical.getObservedCount());
        assertEquals(
                BASE.plusSeconds(10),
                vertical.getDetectedAt()
        );

        assertEquals(
                "DISTINCT_DESTINATION_HOSTS",
                horizontal.getMetricType()
        );
        assertEquals(3, horizontal.getObservedCount());
        assertEquals(
                BASE.plusSeconds(20),
                horizontal.getDetectedAt()
        );

        assertEquals(sensorId, vertical.getSensorId());
        assertEquals(sensorId, horizontal.getSensorId());

        assertEquals(
                vertical.getSourceIp(),
                horizontal.getSourceIp()
        );

        assertNotEquals(
                vertical.getRuleId(),
                horizontal.getRuleId()
        );
    }


    @Test
    void shouldNotAlertOnLowVolumeTraffic()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1101", 0),
                flow(80, "1102", 10),
                flow(22, "1103", 20)
                        .replace("192.0.2.20", "192.0.2.21")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.importReport().rejected());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }


    @Test
    void shouldExpireVerticalScanWindow()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1201", 0),
                flow(80, "1202", 70),
                flow(443, "1203", 140)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.importReport().rejected());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldExpireHorizontalScanWindow()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1301", 0),
                flow(22, "1302", 70)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "1303", 140)
                        .replace("192.0.2.20", "192.0.2.22")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.importReport().rejected());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }


    @Test
    void shouldDetectVerticalScanAtWindowBoundary()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1401", 0),
                flow(80, "1402", 30),
                flow(443, "1403", 60)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());
        assertEquals("NET-001", alerts.getFirst().getRuleId());
        assertEquals(3, alerts.getFirst().getObservedCount());
    }

    @Test
    void shouldDetectHorizontalScanAtWindowBoundary()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1501", 0),
                flow(22, "1502", 30)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "1503", 60)
                        .replace("192.0.2.20", "192.0.2.22")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());
        assertEquals("NET-002", alerts.getFirst().getRuleId());
        assertEquals(3, alerts.getFirst().getObservedCount());
    }

    @Test
    void shouldExpireVerticalScanAfterWindow()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1601", 0),
                flow(80, "1602", 30),
                flow(443, "1603", 61)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }

    @Test
    void shouldExpireHorizontalScanAfterWindow()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1701", 0),
                flow(22, "1702", 30)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "1703", 61)
                        .replace("192.0.2.20", "192.0.2.22")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(3, report.importReport().accepted());
        assertEquals(0, report.alertsPersisted());

        assertTrue(
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                ).isEmpty()
        );
    }


    @Test
    void shouldIgnoreOutOfOrderVerticalScanEvent()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1801", 0),
                flow(80, "1802", 30),
                flow(443, "1803", 20),
                flow(8080, "1804", 50)
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(4, report.importReport().accepted());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());

        DetectionAlertEntity alert = alerts.getFirst();

        assertEquals("NET-001", alert.getRuleId());
        assertEquals(3, alert.getObservedCount());
        assertEquals(BASE.plusSeconds(50),
                alert.getDetectedAt());
    }

    @Test
    void shouldIgnoreOutOfOrderHorizontalScanEvent()
            throws IOException {
        String sensorId = sensor();

        Path path = file(
                flow(22, "1901", 0),
                flow(22, "1902", 30)
                        .replace("192.0.2.20", "192.0.2.21"),
                flow(22, "1903", 20)
                        .replace("192.0.2.20", "192.0.2.22"),
                flow(22, "1904", 50)
                        .replace("192.0.2.20", "192.0.2.23")
        );

        DetectionPipelineReport report =
                pipeline.process(path, sensorId);

        repository.flush();

        assertEquals(4, report.importReport().accepted());
        assertEquals(1, report.alertsPersisted());

        List<DetectionAlertEntity> alerts =
                repository.findBySensorIdOrderByDetectedAtDesc(
                        sensorId
                );

        assertEquals(1, alerts.size());

        DetectionAlertEntity alert = alerts.getFirst();

        assertEquals("NET-002", alert.getRuleId());
        assertEquals(
                "DISTINCT_DESTINATION_HOSTS",
                alert.getMetricType()
        );
        assertEquals(3, alert.getObservedCount());
        assertEquals(BASE.plusSeconds(50),
                alert.getDetectedAt());
    }

}
