
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.PossiblePortScanDetector;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataFlowParser;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataJsonlImporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final int DETECTION_THRESHOLD = 3;

    private final DetectionAlertService alertService;
    private final ImportRunRepository importRuns;

    public SuricataDetectionPipeline(
            DetectionAlertService alertService,
            ImportRunRepository importRuns
    ) {
        this.alertService = Objects.requireNonNull(
                alertService,
                "alertService is required"
        );

        this.importRuns = Objects.requireNonNull(
                importRuns,
                "importRuns is required"
        );
    }

    @Transactional(rollbackFor = IOException.class)
    public DetectionPipelineReport process(
            Path path,
            String sensorId
    ) throws IOException {

        Objects.requireNonNull(
                path, "path is required"
        );

        // Valida o sensor antes de ler o arquivo.
        SuricataFlowParser parser =
                new SuricataFlowParser(sensorId);

        String normalizedSensor = sensorId.trim();

        try (VerifiedImportSnapshot snapshot =
                     VerifiedImportSnapshot.create(path)) {

            String sha256 = snapshot.sha256();

            boolean claimed = importRuns.claim(
                    normalizedSensor,
                    sha256
            );

            if (!claimed) {
                ImportReport previous =
                        importRuns.previousReport(
                                normalizedSensor,
                                sha256
                        );

                snapshot.verifySourceUnchanged();

                return new DetectionPipelineReport(
                        normalizedSensor,
                        previous,
                        0,
                        true
                );
            }

            SuricataJsonlImporter importer =
                    new SuricataJsonlImporter(
                            parser,
                            Clock.systemUTC()
                    );

            PossiblePortScanDetector detector =
                    new PossiblePortScanDetector(
                            DETECTION_WINDOW,
                            DETECTION_THRESHOLD
                    );

            AtomicLong alertsPersisted =
                    new AtomicLong();

            ImportReport report = importer.importFile(
                    snapshot.path(),
                    event -> detector.evaluate(event)
                            .ifPresent(alert -> {
                                alertService.save(alert);

                                alertsPersisted.incrementAndGet();
                            })
            );

            // Impede concluir a importacao se o
            // arquivo original mudou durante o trabalho.
            snapshot.verifySourceUnchanged();

            importRuns.complete(
                    normalizedSensor,
                    sha256,
                    report,
                    alertsPersisted.get()
            );

            return new DetectionPipelineReport(
                    normalizedSensor,
                    report,
                    alertsPersisted.get(),
                    false
            );
        }
    }
}
