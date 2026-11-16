package dev.kayquemiguel.aegis.detection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DetectionRuleRegistryTest {

    private static DetectionRule portScanRule() {
        return new PossiblePortScanDetector(
                Duration.ofSeconds(60),
                3
        );
    }

    @Test
    void shouldRegisterAndFindRule() {
        DetectionRule rule = portScanRule();

        DetectionRuleRegistry registry =
                new DetectionRuleRegistry(List.of(rule));

        assertEquals(1, registry.size());
        assertSame(
                rule,
                registry.findById("NET-001").orElseThrow()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownRule() {
        DetectionRuleRegistry registry =
                new DetectionRuleRegistry(
                        List.of(portScanRule())
                );

        assertEquals(
                Optional.empty(),
                registry.findById("NET-999")
        );
    }

    @Test
    void shouldRejectDuplicateRuleIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DetectionRuleRegistry(
                        List.of(
                                portScanRule(),
                                portScanRule()
                        )
                )
        );
    }

    @Test
    void shouldAllowEmptyRegistry() {
        DetectionRuleRegistry registry =
                new DetectionRuleRegistry(List.of());

        assertEquals(0, registry.size());
        assertTrue(registry.findAll().isEmpty());
    }

    @Test
    void shouldRejectNullRules() {
        assertThrows(
                NullPointerException.class,
                () -> new DetectionRuleRegistry(null)
        );
    }

    @Test
    void shouldExposeRulesInStableOrder() {
        DetectionRule first = portScanRule();

        DetectionRuleRegistry registry =
                new DetectionRuleRegistry(List.of(first));

        assertEquals(
                List.of("NET-001"),
                registry.findAll().stream()
                        .map(DetectionRule::ruleId)
                        .toList()
        );
    }
}
