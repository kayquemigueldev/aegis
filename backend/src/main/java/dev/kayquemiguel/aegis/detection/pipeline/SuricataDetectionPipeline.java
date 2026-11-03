
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.PossiblePortScanDetector;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataFlowParser;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataJsonlImporter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SuricataDetectionPipeline {

    private static final Duration DETECTION_WINDOW =
            Duration.ofSeconds(60);

    // Valor experimental, apenas para esta fase.
    private static final int DETECTION_THRESHOLD = 3;

    private final DetectionAlertService alertService;

    public SuricataDetectionPipeline(
            DetectionAlertService alertService
    ) {
        this.alertService = Objects.requireNonNull(
                alertService, "alertService is required"
        );
    }

    public DetectionPipelineReport process(
            Path path,
            String sensorId
    ) throws IOException {
        Objects.requireNonNull(path, "path is required");

        // O construtor valida o identificador do sensor.
        SuricataFlowParser parser =
                new SuricataFlowParser(sensorId);

        SuricataJsonlImporter importer =
                new SuricataJsonlImporter(
                        parser,
                        Clock.systemUTC()
                );

        // Um detector novo por importacao.
        // Evita misturar estados entre arquivos.
        PossiblePortScanDetector detector =
                new PossiblePortScanDetector(
                        DETECTION_WINDOW,
                        DETECTION_THRESHOLD
                );

        AtomicLong alertsPersisted = new AtomicLong();

        ImportReport importReport = importer.importFile(
                path,
                event -> detector.evaluate(event)
                        .ifPresent(alert -> {
                            alertService.save(alert);
                            alertsPersisted.incrementAndGet();
                        })
        );

        return new DetectionPipelineReport(
                sensorId.trim(),
                importReport,
                alertsPersisted.get()
        );
    }
}
