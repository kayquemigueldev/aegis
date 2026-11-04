
package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
class ImportFailureRecoveryTest {

    @Autowired
    private SuricataDetectionPipeline pipeline;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private DetectionAlertService alertService;

    @TempDir
    Path tempDir;

    private static String sensor() {
        return "test-" + UUID.randomUUID();
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

    private long count(
            String table,
            String sensorId
    ) {
        String sql = switch (table) {
            case "detection_alerts" ->
                    "SELECT COUNT(*) FROM detection_alerts "
                            + "WHERE sensor_id = ?";
            case "import_runs" ->
                    "SELECT COUNT(*) FROM import_runs "
                            + "WHERE sensor_id = ?";
            default -> throw new IllegalArgumentException(
                    "Unsupported table"
            );
        };

        Long result = jdbc.queryForObject(
                sql,
                Long.class,
                sensorId
        );

        return result == null ? 0 : result;
    }

    private void assertNoRecords(String sensorId) {
        assertEquals(
                0,
                count("detection_alerts", sensorId)
        );

        assertEquals(
                0,
                count("import_runs", sensorId)
        );
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
    void shouldRollbackWhenAlertPersistenceFails()
            throws Exception {

        String sensorId = sensor();
        Path path = createFile();

        try {
            doThrow(
                    new IllegalStateException(
                            "Injected persistence failure"
                    )
            ).when(alertService).save(any());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> pipeline.process(
                                    path,
                                    sensorId
                            )
                    );

            assertEquals(
                    "Injected persistence failure",
                    exception.getMessage()
            );

            // A transacao do pipeline deve ser revertida.
            assertNoRecords(sensorId);

        } finally {
            reset(alertService);
            cleanup(sensorId);
        }
    }

    @Test
    void shouldRollbackIfSourceChangesDuringImport()
            throws Exception {

        String sensorId = sensor();
        Path path = createFile();

        try {
            doAnswer(invocation -> {

                Object result =
                        invocation.callRealMethod();

                // Simula outro processo alterando
                // o arquivo original durante a importacao.
                Files.writeString(
                        path,
                        "{\"event_type\":\"alert\"}\n"
                );

                return result;

            }).when(alertService).save(any());

            IOException exception =
                    assertThrows(
                            IOException.class,
                            () -> pipeline.process(
                                    path,
                                    sensorId
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("Source changed")
            );

            // Mesmo apos tentar gravar o alerta,
            // nenhuma alteracao deve ser confirmada.
            assertNoRecords(sensorId);

        } finally {
            reset(alertService);
            cleanup(sensorId);
        }
    }
}
