
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImportResilienceTest {

    @Autowired
    private SuricataDetectionPipeline pipeline;

    @Autowired
    private DetectionAlertRepository alerts;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @TempDir
    Path tempDir;

    private static String sensor() {
        return "test-" + UUID.randomUUID();
    }

    private static String flow(
            int port,
            int flowId,
            int second
    ) {
        return """
                {"timestamp":"2026-11-03T10:00:%02dZ",
                "event_type":"flow",
                "src_ip":"192.0.2.10",
                "dest_ip":"192.0.2.20",
                "src_port":49152,
                "dest_port":%d,
                "proto":"TCP",
                "flow_id":%d}
                """.formatted(
                second,
                port,
                flowId
        ).replace("\n", "");
    }

    private Path createFile() throws IOException {
        Path path = tempDir.resolve(
                UUID.randomUUID() + ".jsonl"
        );

        Files.write(
                path,
                List.of(
                        flow(22, 1001, 0),
                        flow(80, 1002, 10),
                        flow(443, 1003, 20)
                )
        );

        return path;
    }

    private long alertCount(String sensorId) {
        return alerts
                .findBySensorIdOrderByDetectedAtDesc(sensorId)
                .size();
    }

    private long runCount(String sensorId) {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM import_runs
                WHERE sensor_id = ?
                """,
                Long.class,
                sensorId
        );

        return count == null ? 0 : count;
    }

    private void cleanup(String sensorId) {
        jdbc.update(
                "DELETE FROM detection_alerts WHERE sensor_id = ?",
                sensorId
        );

        jdbc.update(
                "DELETE FROM import_runs WHERE sensor_id = ?",
                sensorId
        );
    }

    @Test
    void shouldRollbackAlertsAndImportRunWhenTransactionFails()
            throws Exception {

        String sensorId = sensor();
        Path path = createFile();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> transaction.execute(status -> {
                        try {
                            DetectionPipelineReport report =
                                    pipeline.process(path, sensorId);

                            assertEquals(
                                    1,
                                    report.alertsPersisted()
                            );

                            // A falha ocorre antes do COMMIT.
                            throw new IllegalStateException(
                                    "Simulated failure before commit"
                            );

                        } catch (IOException exception1) {
                            throw new UncheckedIOException(exception1);
                        }
                    })
            );

            assertEquals(
                    "Simulated failure before commit",
                    exception.getMessage()
            );

            // Consultas fora da transacao abortada.
            assertEquals(0, alertCount(sensorId));
            assertEquals(0, runCount(sensorId));

        } finally {
            cleanup(sensorId);
        }
    }

    @Test
    void shouldRetryAfterRollback()
            throws Exception {

        String sensorId = sensor();
        Path path = createFile();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        try {
            assertThrows(
                    IllegalStateException.class,
                    () -> transaction.execute(status -> {
                        try {
                            pipeline.process(path, sensorId);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }

                        throw new IllegalStateException(
                                "Abort first attempt"
                        );
                    })
            );

            assertEquals(0, alertCount(sensorId));
            assertEquals(0, runCount(sensorId));

            // Uma nova tentativa deve funcionar.
            DetectionPipelineReport retry =
                    pipeline.process(path, sensorId);

            assertFalse(retry.alreadyImported());
            assertEquals(1, retry.alertsPersisted());

            assertEquals(1, alertCount(sensorId));
            assertEquals(1, runCount(sensorId));

        } finally {
            cleanup(sensorId);
        }
    }

    @Test
    void shouldPreventDuplicateConcurrentImports()
            throws Exception {

        String sensorId = sensor();
        Path path = createFile();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Callable<DetectionPipelineReport>
                    task = () -> {

                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to start"
                    );
                }

                return pipeline.process(path, sensorId);
            };

            Future<DetectionPipelineReport> first =
                    executor.submit(task);

            Future<DetectionPipelineReport> second =
                    executor.submit(task);

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both workers must be ready"
            );

            // Libera as duas threads simultaneamente.
            start.countDown();

            DetectionPipelineReport firstResult =
                    first.get(20, TimeUnit.SECONDS);

            DetectionPipelineReport secondResult =
                    second.get(20, TimeUnit.SECONDS);

            long newImports = List.of(
                            firstResult,
                            secondResult
                    ).stream()
                    .filter(result -> !result.alreadyImported())
                    .count();

            long duplicates = List.of(
                            firstResult,
                            secondResult
                    ).stream()
                    .filter(DetectionPipelineReport::alreadyImported)
                    .count();

            assertEquals(1, newImports);
            assertEquals(1, duplicates);

            assertEquals(
                    1,
                    firstResult.alertsPersisted()
                            + secondResult.alertsPersisted()
            );

            assertEquals(1, alertCount(sensorId));
            assertEquals(1, runCount(sensorId));

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Workers must finish before cleanup"
            );

            cleanup(sensorId);
        }
    }

    @Test
    void shouldPersistSeparateImportsForDifferentSensors()
            throws Exception {

        Path path = createFile();

        String sensorA = sensor();
        String sensorB = sensor();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<DetectionPipelineReport> first =
                    executor.submit(
                            () -> pipeline.process(path, sensorA)
                    );

            Future<DetectionPipelineReport> second =
                    executor.submit(
                            () -> pipeline.process(path, sensorB)
                    );

            DetectionPipelineReport resultA =
                    first.get(20, TimeUnit.SECONDS);

            DetectionPipelineReport resultB =
                    second.get(20, TimeUnit.SECONDS);

            assertFalse(resultA.alreadyImported());
            assertFalse(resultB.alreadyImported());

            assertEquals(1, alertCount(sensorA));
            assertEquals(1, alertCount(sensorB));

            assertEquals(1, runCount(sensorA));
            assertEquals(1, runCount(sensorB));

        } finally {
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS)
            );

            cleanup(sensorA);
            cleanup(sensorB);
        }
    }
}
