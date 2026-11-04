
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.PossiblePortScanDetector;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataFlowParser;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataJsonlImporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
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
                alertService, "alertService is required"
        );
        this.importRuns = Objects.requireNonNull(
                importRuns, "importRuns is required"
        );
    }

    @Transactional(rollbackFor = IOException.class)
    public DetectionPipelineReport process(
            Path path,
            String sensorId
    ) throws IOException {
        Objects.requireNonNull(path, "path is required");

        SuricataFlowParser parser =
                new SuricataFlowParser(sensorId);

        String normalizedSensor = sensorId.trim();
        String sha256 = calculateSha256(path);

        // Esta operacao e protegida pela restricao UNIQUE
        // no PostgreSQL e participa da transacao atual.
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

        // Mantemos um detector independente por arquivo.
        PossiblePortScanDetector detector =
                new PossiblePortScanDetector(
                        DETECTION_WINDOW,
                        DETECTION_THRESHOLD
                );

        AtomicLong alertsPersisted = new AtomicLong();

        ImportReport report = importer.importFile(
                path,
                event -> detector.evaluate(event)
                        .ifPresent(alert -> {
                            alertService.save(alert);
                            alertsPersisted.incrementAndGet();
                        })
        );

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

    private static String calculateSha256(
            Path path
    ) throws IOException {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }

        byte[] buffer = new byte[8192];

        try (InputStream input = Files.newInputStream(path)) {
            int count;

            while ((count = input.read(buffer)) != -1) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}
