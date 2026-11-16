package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DetectionEngineTest {

    private static NetworkEvent event() {
        Instant now = Instant.parse(
                "2026-11-16T12:00:00Z"
        );

        return new NetworkEvent(
                UUID.randomUUID(),
                now,
                now.plusSeconds(1),
                "192.0.2.10",
                "192.0.2.20",
                49152,
                443,
                NetworkProtocol.TCP,
                "suricata",
                "flow-001"
        );
    }

    private static DetectionRule rule(
            String id,
            boolean generateAlert
    ) {
        return new DetectionRule() {

            @Override
            public String ruleId() {
                return id;
            }

            @Override
            public Optional<DetectionAlert> evaluate(
                    NetworkEvent event
            ) {
                if (!generateAlert) {
                    return Optional.empty();
                }

                return Optional.of(new DetectionAlert(
                        id,
                        event.sourceIp(),
                        event.destinationIp(),
                        event.occurredAt(),
                        1,
                        1,
                        "Test detection"
                ));
            }
        };
    }

    @Test
    void shouldExecuteMultipleRules() {
        DetectionEngine engine = new DetectionEngine(
                new DetectionRuleRegistry(List.of(
                        rule("NET-002", true),
                        rule("NET-001", true)
                ))
        );

        List<DetectionAlert> alerts =
                engine.evaluate(event());

        assertEquals(2, engine.ruleCount());
        assertEquals(2, alerts.size());
        assertEquals("NET-001", alerts.get(0).ruleId());
        assertEquals("NET-002", alerts.get(1).ruleId());
    }

    @Test
    void shouldIgnoreRulesWithoutAlerts() {
        DetectionEngine engine = new DetectionEngine(
                new DetectionRuleRegistry(List.of(
                        rule("NET-001", false),
                        rule("NET-002", true)
                ))
        );

        List<DetectionAlert> alerts =
                engine.evaluate(event());

        assertEquals(1, alerts.size());
        assertEquals("NET-002", alerts.get(0).ruleId());
    }

    @Test
    void shouldSupportEmptyRegistry() {
        DetectionEngine engine = new DetectionEngine(
                new DetectionRuleRegistry(List.of())
        );

        assertEquals(0, engine.ruleCount());
        assertTrue(engine.evaluate(event()).isEmpty());
    }

    @Test
    void shouldRejectInvalidArguments() {
        assertThrows(
                NullPointerException.class,
                () -> new DetectionEngine(null)
        );

        DetectionEngine engine = new DetectionEngine(
                new DetectionRuleRegistry(List.of())
        );

        assertThrows(
                NullPointerException.class,
                () -> engine.evaluate(null)
        );
    }
}
