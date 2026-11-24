package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DetectionContractTest {

    private static final Instant NOW =
            Instant.parse("2026-11-24T10:00:00Z");

    private static DetectionRule rule(String id) {
        return new DetectionRule() {
            @Override
            public String ruleId() {
                return id;
            }

            @Override
            public Optional<DetectionAlert> evaluate(
                    NetworkEvent event
            ) {
                return Optional.empty();
            }
        };
    }

    @Test
    void shouldRejectDuplicateRuleIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionRuleRegistry(
                        List.of(
                                rule("NET-001"),
                                rule("NET-001")
                        )
                )
        );
    }

    @Test
    void shouldReturnRulesInStableOrder() {
        DetectionRuleRegistry registry =
                new DetectionRuleRegistry(
                        List.of(
                                rule("NET-002"),
                                rule("NET-001")
                        )
                );

        assertEquals(
                List.of("NET-001", "NET-002"),
                registry.findAll().stream()
                        .map(DetectionRule::ruleId)
                        .toList()
        );
    }

    @Test
    void shouldRejectInvalidRuleId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionRuleRegistry(
                        List.of(rule("invalid rule id"))
                )
        );
    }

    @Test
    void shouldPreserveAlertMetadata() {
        DetectionAlert alert = new DetectionAlert(
                "NET-002",
                "192.0.2.10",
                "198.51.100.20",
                NOW,
                7,
                5,
                "Horizontal scan",
                "sensor-01",
                DetectionMetricType
                        .DISTINCT_DESTINATION_HOSTS
        );

        assertEquals("NET-002", alert.ruleId());
        assertEquals("sensor-01", alert.sensorId());
        assertEquals(7, alert.observedCount());
        assertEquals(5, alert.threshold());
        assertEquals(
                DetectionMetricType
                        .DISTINCT_DESTINATION_HOSTS,
                alert.metricType()
        );
    }

    @Test
    void shouldMapNetworkAlertToPersistence() {
        DetectionAlert alert = new DetectionAlert(
                "NET-001",
                "192.0.2.10",
                "198.51.100.20",
                NOW,
                10,
                8,
                "Possible port scan",
                "sensor-01",
                DetectionMetricType
                        .DISTINCT_DESTINATION_PORTS
        );

        DetectionAlertEntity entity =
                DetectionAlertEntity.from(alert);

        assertNotNull(entity.getId());
        assertEquals("NET-001", entity.getRuleId());
        assertEquals("sensor-01",
                entity.getSensorId());
        assertEquals(10, entity.getObservedCount());
        assertEquals(8,
                entity.getDetectionThreshold());

        DetectionAlert restored = entity.toDomain();

        assertEquals(alert.ruleId(),
                restored.ruleId());
        assertEquals(alert.metricType(),
                restored.metricType());
        assertEquals(alert.sensorId(),
                restored.sensorId());
    }

    @Test
    void shouldRejectInvalidAlertThreshold() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionAlert(
                        "NET-001",
                        "192.0.2.10",
                        "198.51.100.20",
                        NOW,
                        10,
                        0,
                        "Invalid threshold",
                        "sensor-01",
                        DetectionMetricType
                                .DISTINCT_DESTINATION_PORTS
                )
        );
    }
}
