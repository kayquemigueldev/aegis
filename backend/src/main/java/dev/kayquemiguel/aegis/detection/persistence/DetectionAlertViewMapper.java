package dev.kayquemiguel.aegis.detection.persistence;

import dev.kayquemiguel.aegis.detection.application.DetectionAlertView;

import java.util.Objects;

/**
 * Maps a persisted detection alert to an application read model.
 */
public final class DetectionAlertViewMapper {

    private DetectionAlertViewMapper() {
    }

    public static DetectionAlertView from(
            DetectionAlertEntity entity
    ) {
        Objects.requireNonNull(entity, "entity is required");

        return new DetectionAlertView(
                entity.getId(),
                entity.getRuleId(),
                entity.getSensorId(),
                entity.getSourceIp(),
                entity.getDestinationIp(),
                entity.getDetectedAt(),
                entity.getDistinctPorts(),
                entity.getDetectionThreshold(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getMetricType(),
                entity.getObservedCount(),
                entity.getAuthUsername(),
                entity.getAuthSourceSystem()
        );
    }
}
