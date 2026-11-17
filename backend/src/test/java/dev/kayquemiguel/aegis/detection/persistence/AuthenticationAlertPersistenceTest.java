package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
import dev.kayquemiguel.aegis.detection.DetectionAlert;
import dev.kayquemiguel.aegis.detection.DetectionMetricType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthenticationAlertPersistenceTest {

    @Autowired
    private DetectionAlertService service;

    @Autowired
    private DetectionAlertRepository repository;

    @Test
    void shouldPersistAndReadNet003Alert() {
        AuthenticationDetectionAlert alert =
                new AuthenticationDetectionAlert(
                        "NET-003",
                        Instant.parse("2026-11-16T10:00:00Z"),
                        "sensor-001",
                        "auth-log",
                        "192.0.2.10",
                        "admin",
                        5,
                        5
                );

        UUID id = service.saveAuthentication(alert);

        repository.flush();

        DetectionAlertEntity saved =
                repository.findById(id).orElseThrow();

        assertEquals("NET-003", saved.getRuleId());
        assertEquals(
                "AUTHENTICATION_FAILURES",
                saved.getMetricType()
        );
        assertNull(saved.getDestinationIp());
        assertEquals(0, saved.getDistinctPorts());
        assertEquals(5, saved.getObservedCount());
        assertEquals("admin", saved.getAuthUsername());
        assertEquals(
                "auth-log", saved.getAuthSourceSystem()
        );

        AuthenticationDetectionAlert restored =
                saved.toAuthenticationDomain();

        assertEquals(alert, restored);
    }

    @Test
    void shouldPreserveLegacyNetworkAlertPersistence() {
        DetectionAlert alert = new DetectionAlert(
                "NET-001",
                "192.0.2.10",
                "192.0.2.20",
                Instant.parse("2026-11-16T10:00:00Z"),
                3,
                3,
                "Possible vertical scan",
                "sensor-001",
                DetectionMetricType.DISTINCT_DESTINATION_PORTS
        );

        UUID id = service.save(alert);

        repository.flush();

        DetectionAlertEntity saved =
                repository.findById(id).orElseThrow();

        assertEquals("NET-001", saved.getRuleId());
        assertEquals(
                "192.0.2.20", saved.getDestinationIp()
        );
        assertNull(saved.getAuthUsername());
        assertEquals(alert, saved.toDomain());
    }

    @Test
    void shouldKeepLegacyQueriesSafeWithNet003() {
        String sensorId =
                "test-" + UUID.randomUUID();

        AuthenticationDetectionAlert authenticationAlert =
                new AuthenticationDetectionAlert(
                        "NET-003",
                        Instant.parse("2026-11-16T11:00:00Z"),
                        sensorId,
                        "auth-log",
                        "192.0.2.10",
                        "admin",
                        5,
                        5
                );

        service.saveAuthentication(authenticationAlert);

        DetectionAlert networkAlert = new DetectionAlert(
                "NET-001",
                "192.0.2.10",
                "192.0.2.20",
                Instant.parse("2026-11-16T11:01:00Z"),
                3,
                3,
                "Possible vertical scan",
                sensorId,
                DetectionMetricType.DISTINCT_DESTINATION_PORTS
        );

        service.save(networkAlert);
        repository.flush();

        var networkResults =
                service.findBySensor(sensorId);

        assertEquals(1, networkResults.size());
        assertEquals("NET-001",
                networkResults.get(0).ruleId());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findByRule("NET-003")
        );
    }

}
