package dev.kayquemiguel.aegis.detection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DetectionRuleContractTest {

    @Test
    void shouldExposeRuleIdentifier() {
        DetectionRule rule =
                new PossiblePortScanDetector(
                        Duration.ofSeconds(60),
                        3
                );

        assertEquals("NET-001", rule.ruleId());
    }
}
