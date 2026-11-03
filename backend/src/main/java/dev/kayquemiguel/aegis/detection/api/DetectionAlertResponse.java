
package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertEntity;

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
        Instant createdAt
) {
    public static DetectionAlertResponse from(
            DetectionAlertEntity entity
    ) {
        return new DetectionAlertResponse(
                entity.getId(),
                entity.getRuleId(),
                entity.getSensorId(),
                entity.getSourceIp(),
                entity.getDestinationIp(),
                entity.getDetectedAt(),
                entity.getDistinctPorts(),
                entity.getDetectionThreshold(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
