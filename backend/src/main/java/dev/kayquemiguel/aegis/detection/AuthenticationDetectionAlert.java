package dev.kayquemiguel.aegis.detection;

import java.time.Instant;

public record AuthenticationDetectionAlert(
        String ruleId,
        Instant detectedAt,
        String sensorId,
        String sourceSystem,
        String sourceIp,
        String username,
        int observedCount,
        int threshold
) {
}
