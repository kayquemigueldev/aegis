
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.ingestion.suricata.ImportReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
public class SuricataImportRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SuricataImportRunner.class);

    private final SuricataDetectionPipeline pipeline;
    private final String file;
    private final String sensor;

    public SuricataImportRunner(
            SuricataDetectionPipeline pipeline,
            @Value("${aegis.import.file:}") String file,
            @Value("${aegis.import.sensor:}") String sensor
    ) {
        this.pipeline = Objects.requireNonNull(
                pipeline,
                "pipeline is required"
        );
        this.file = Objects.requireNonNull(file);
        this.sensor = Objects.requireNonNull(sensor);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // Sem configuracao de importacao:
        // comportamento normal do backend permanece igual.
        if (file.isBlank()) {
            if (!sensor.isBlank()) {
                throw new IllegalArgumentException(
                        "aegis.import.file is required "
                                + "when aegis.import.sensor is provided"
                );
            }
            return;
        }

        if (sensor.isBlank()) {
            throw new IllegalArgumentException(
                    "aegis.import.sensor is required"
            );
        }

        Path path = Path.of(file.trim())
                .toAbsolutePath()
                .normalize();

        log.info("AEGIS import started");
        log.info("Sensor: {}", sensor.trim());
        log.info("Input file: {}", path);

        DetectionPipelineReport report =
                pipeline.process(path, sensor.trim());

        ImportReport imported = report.importReport();


        if (report.alreadyImported()) {
            log.info("Import skipped: file already processed");
            log.info("Sensor: {}", report.sensorId());
            log.info(
                    "Previously accepted events: {}",
                    imported.accepted()
            );
            return;
        }


        log.info("========== AEGIS IMPORT REPORT ==========");
        log.info("Sensor: {}", report.sensorId());
        log.info("Total lines: {}", imported.totalLines());
        log.info("Accepted events: {}", imported.accepted());
        log.info("Ignored events: {}", imported.ignored());
        log.info("Rejected events: {}", imported.rejected());
        log.info("Persisted alerts: {}", report.alertsPersisted());

        for (ImportReport.RejectedLine error :
                imported.errors()) {
            log.warn(
                    "Rejected line {}: {}",
                    error.lineNumber(),
                    error.reason()
            );
        }

        log.info("========== IMPORT COMPLETED ==========");
    }
}
