
package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.application.DetectionAlertView;

import java.time.Instant;
import java.util.UUID;

public record DetectionAlertResponse(
        UUID id,
        String ruleId,
        String sensorId,
        String sourceIp,
        String destinationIp,
        Instant detectedAt,
        int distinctPorts,
        int threshold,
        String description,
        Instant createdAt,
        String metricType,
        int observedCount,
        String authUsername,
        String authSourceSystem
) {
    public static DetectionAlertResponse from(
            DetectionAlertView entity
    ) {
        return new DetectionAlertResponse(
                entity.id(),
                entity.ruleId(),
                entity.sensorId(),
                entity.sourceIp(),
                entity.destinationIp(),
                entity.detectedAt(),
                entity.distinctPorts(),
                entity.threshold(),
                entity.description(),
                entity.createdAt(),
                entity.metricType(),
                entity.observedCount(),
                entity.authUsername(),
                entity.authSourceSystem()
        );
    }
}
