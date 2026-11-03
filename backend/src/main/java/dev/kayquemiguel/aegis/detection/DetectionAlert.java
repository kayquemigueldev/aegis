
package dev.kayquemiguel.aegis.detection;

import java.time.Instant;
import java.util.Objects;

public record DetectionAlert(
        String ruleId,
        String sourceIp,
        String destinationIp,
        Instant detectedAt,
        int distinctPorts,
        int threshold,
        String description
) {

    public DetectionAlert {
        Objects.requireNonNull(ruleId);
        Objects.requireNonNull(sourceIp);
        Objects.requireNonNull(destinationIp);
        Objects.requireNonNull(detectedAt);
        Objects.requireNonNull(description);

        if (distinctPorts < 1 || threshold < 1) {
            throw new IllegalArgumentException(
                    "Detection counts must be positive"
            );
        }
    }
}
