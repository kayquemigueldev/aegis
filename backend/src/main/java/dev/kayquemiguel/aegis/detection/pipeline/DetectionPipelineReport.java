
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;

import java.util.Objects;

public record DetectionPipelineReport(
        String sensorId,
        ImportReport importReport,
        long alertsPersisted,
        boolean alreadyImported
) {

    // Mantem compatibilidade com os testes existentes.
    public DetectionPipelineReport(
            String sensorId,
            ImportReport importReport,
            long alertsPersisted
    ) {
        this(
                sensorId,
                importReport,
                alertsPersisted,
                false
        );
    }

    public DetectionPipelineReport {
        Objects.requireNonNull(
                sensorId, "sensorId is required"
        );
        Objects.requireNonNull(
                importReport, "importReport is required"
        );

        if (sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId must not be blank"
            );
        }

        if (alertsPersisted < 0
                || alertsPersisted > importReport.accepted()) {
            throw new IllegalArgumentException(
                    "Invalid persisted alert count"
            );
        }

        if (alreadyImported && alertsPersisted != 0) {
            throw new IllegalArgumentException(
                    "Duplicate import must not persist alerts"
            );
        }
    }
}
