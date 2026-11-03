
package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DetectionAlertPersistenceTest {

    @Autowired
    private DetectionAlertService service;

    @Autowired
    private DetectionAlertRepository repository;

    private static DetectionAlert alert(
            String sensorId,
            String ruleId,
            Instant detectedAt
    ) {
        return new DetectionAlert(
                ruleId,
                "192.0.2.10",
                "192.0.2.20",
                detectedAt,
                5,
                3,
                "Synthetic detection for integration testing",
                sensorId
        );
    }

    @Test
    void shouldPersistAlertInPostgres() {
        String sensor = uniqueSensor();

        DetectionAlert original = alert(
                sensor,
                "NET-001",
                Instant.parse("2026-11-03T10:00:00Z")
        );

        UUID id = service.save(original);
        repository.flush();

        DetectionAlertEntity persisted =
                repository.findById(id).orElseThrow();

        assertEquals(id, persisted.getId());
        assertEquals("NET-001", persisted.getRuleId());
        assertEquals(sensor, persisted.getSensorId());
        assertEquals("192.0.2.10", persisted.getSourceIp());
        assertEquals("192.0.2.20", persisted.getDestinationIp());
        assertEquals(5, persisted.getDistinctPorts());
        assertEquals(3, persisted.getDetectionThreshold());
        assertEquals(original.detectedAt(),
                persisted.getDetectedAt());
        assertEquals(original.description(),
                persisted.getDescription());
    }

    @Test
    void shouldRecoverPersistedDomainAlert() {
        String sensor = uniqueSensor();

        DetectionAlert original = alert(
                sensor,
                "NET-001",
                Instant.parse("2026-11-03T10:01:00Z")
        );

        UUID id = service.save(original);
        repository.flush();

        DetectionAlert recovered =
                repository.findById(id)
                        .orElseThrow()
                        .toDomain();

        assertEquals(original, recovered);
    }

    @Test
    void shouldFilterAndOrderAlertsBySensor() {
        String sensorA = uniqueSensor();
        String sensorB = uniqueSensor();

        Instant base =
                Instant.parse("2026-11-03T10:00:00Z");

        service.save(alert(
                sensorA, "NET-001", base
        ));

        service.save(alert(
                sensorB, "NET-001", base.plusSeconds(5)
        ));

        service.save(alert(
                sensorA, "NET-001", base.plusSeconds(10)
        ));

        repository.flush();

        List<DetectionAlert> results =
                service.findBySensor(sensorA);

        assertEquals(2, results.size());
        assertEquals(
                base.plusSeconds(10),
                results.get(0).detectedAt()
        );
        assertEquals(base, results.get(1).detectedAt());

        assertTrue(results.stream().allMatch(
                result -> result.sensorId().equals(sensorA)
        ));
    }

    @Test
    void shouldFilterAlertsByRule() {
        String sensor = uniqueSensor();
        String customRule = "TEST-" + UUID.randomUUID();

        Instant base =
                Instant.parse("2026-11-03T10:00:00Z");

        service.save(alert(
                sensor, customRule, base
        ));

        service.save(alert(
                sensor, "NET-001", base.plusSeconds(1)
        ));

        repository.flush();

        List<DetectionAlert> results =
                service.findByRule(customRule);

        assertEquals(1, results.size());
        assertEquals(customRule, results.get(0).ruleId());
        assertEquals(sensor, results.get(0).sensorId());
    }

    @Test
    void shouldRejectInvalidSearchParameters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findBySensor(" ")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByRule(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> service.save(null)
        );
    }

    private static String uniqueSensor() {
        return "test-" + UUID.randomUUID();
    }
}
