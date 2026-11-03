
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;

import java.time.Instant;
import java.util.Objects;

public record DetectionAlert(
        String ruleId,
        String sourceIp,
        String destinationIp,
        Instant detectedAt,
        int distinctPorts,
        int threshold,
        String description,
        String sensorId
) {

    public DetectionAlert(
            String ruleId,
            String sourceIp,
            String destinationIp,
            Instant detectedAt,
            int distinctPorts,
            int threshold,
            String description
    ) {
        this(ruleId, sourceIp, destinationIp,
                detectedAt, distinctPorts,
                threshold, description,
                NetworkEvent.LEGACY_SENSOR_ID);
    }

    public DetectionAlert {
        Objects.requireNonNull(ruleId, "ruleId is required");
        Objects.requireNonNull(sourceIp, "sourceIp is required");
        Objects.requireNonNull(destinationIp, "destinationIp is required");
        Objects.requireNonNull(detectedAt, "detectedAt is required");
        Objects.requireNonNull(description, "description is required");

        if (sensorId == null || sensorId.isBlank()
                || !sensorId.trim().matches(
                "[A-Za-z0-9][A-Za-z0-9._-]{0,63}"
        )) {
            throw new IllegalArgumentException(
                    "Invalid sensorId"
            );
        }

        sensorId = sensorId.trim();

        if (distinctPorts < 1 || threshold < 1) {
            throw new IllegalArgumentException(
                    "Detection counts must be positive"
            );
        }
    }
}
