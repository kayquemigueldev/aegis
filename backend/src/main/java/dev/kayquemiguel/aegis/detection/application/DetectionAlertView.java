package dev.kayquemiguel.aegis.detection.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read model for persisted detection alerts.
 *
 * Supports both network and authentication alerts
 * without exposing JPA entities to application consumers.
 */
public record DetectionAlertView(
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
}
