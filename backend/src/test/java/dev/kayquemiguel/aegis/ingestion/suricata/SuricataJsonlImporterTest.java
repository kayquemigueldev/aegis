
package dev.kayquemiguel.aegis.ingestion.suricata;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuricataJsonlImporterTest {

    @TempDir
    Path tempDir;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-31T12:00:00Z"),
            ZoneOffset.UTC
    );

    private final SuricataJsonlImporter importer =
            new SuricataJsonlImporter(
                    new SuricataFlowParser(),
                    clock
            );

    private static String flow(int port) {
        return """
                {"timestamp":"2026-10-31T10:00:00Z",
                "event_type":"flow",
                "src_ip":"192.0.2.10",
                "dest_ip":"192.0.2.20",
                "src_port":49152,
                "dest_port":%d,
                "proto":"TCP"}
                """.formatted(port).replace("\n", "");
    }

    @Test
    void shouldImportMixedFile() throws IOException {
        Path file = tempDir.resolve("eve.jsonl");

        Files.write(file, List.of(
                flow(80),
                "{\"event_type\":\"alert\"}",
                "{invalid}",
                flow(443),
                ""
        ));

        List<NetworkEvent> events = new ArrayList<>();

        ImportReport report = importer.importFile(
                file,
                events::add
        );

        assertEquals(5, report.totalLines());
        assertEquals(2, report.accepted());
        assertEquals(1, report.ignored());
        assertEquals(2, report.rejected());
        assertEquals(2, report.errors().size());
        assertEquals(3, report.errors().get(0).lineNumber());
        assertEquals(5, report.errors().get(1).lineNumber());
        assertEquals(2, events.size());
        assertEquals(80, events.get(0).destinationPort());
        assertEquals(443, events.get(1).destinationPort());
    }

    @Test
    void shouldReturnEmptyReportForEmptyFile()
            throws IOException {

        Path file = tempDir.resolve("empty.jsonl");
        Files.writeString(file, "");

        ImportReport report = importer.importFile(
                file,
                event -> fail("No event expected")
        );

        assertEquals(0, report.totalLines());
        assertEquals(0, report.accepted());
        assertTrue(report.errors().isEmpty());
    }

    @Test
    void shouldRejectInvalidPorts() throws IOException {
        Path file = tempDir.resolve("invalid.jsonl");
        Files.writeString(file, flow(70000) + "\n");

        ImportReport report = importer.importFile(
                file,
                event -> fail("Invalid event must not be accepted")
        );

        assertEquals(1, report.rejected());
        assertEquals(0, report.accepted());
    }

    @Test
    void shouldPropagateConsumerFailure() throws IOException {
        Path file = tempDir.resolve("consumer.jsonl");
        Files.writeString(file, flow(443) + "\n");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> importer.importFile(file, event -> {
                    throw new IllegalStateException("Consumer failed");
                })
        );

        assertEquals("Consumer failed", exception.getMessage());
    }

    @Test
    void shouldRejectMissingFile() {
        Path file = tempDir.resolve("missing.jsonl");

        assertThrows(IOException.class, () ->
                importer.importFile(file, event -> {})
        );
    }

    @Test
    void shouldRejectOversizedLineAndContinue()
            throws IOException {

        Path file = tempDir.resolve("oversized.jsonl");

        String oversized = "x".repeat(
                SuricataJsonlImporter.MAX_LINE_LENGTH + 1
        );

        Files.writeString(
                file,
                oversized + "\n" + flow(443) + "\n"
        );

        List<NetworkEvent> events = new ArrayList<>();

        ImportReport report = importer.importFile(
                file,
                events::add
        );

        assertEquals(2, report.totalLines());
        assertEquals(1, report.rejected());
        assertEquals(1, report.accepted());
        assertEquals(0, report.ignored());
        assertEquals(1, report.errors().get(0).lineNumber());
        assertEquals(1, events.size());
    }

    @Test
    void shouldProcessValidLineAfterMultipleOversizedLines()
            throws IOException {

        Path file = tempDir.resolve("multiple-large.jsonl");

        String oversized = "x".repeat(
                SuricataJsonlImporter.MAX_LINE_LENGTH + 100
        );

        Files.writeString(
                file,
                oversized + "\n"
                        + oversized + "\n"
                        + flow(80) + "\n"
        );

        ImportReport report = importer.importFile(
                file,
                event -> {}
        );

        assertEquals(3, report.totalLines());
        assertEquals(2, report.rejected());
        assertEquals(1, report.accepted());
        assertEquals(2, report.errors().size());
    }

    @Test
    void shouldLimitStoredErrors() throws IOException {
        Path file = tempDir.resolve("many-errors.jsonl");

        StringBuilder input = new StringBuilder();

        for (int i = 0; i < 150; i++) {
            input.append("{invalid}\n");
        }

        Files.writeString(file, input.toString());

        ImportReport report = importer.importFile(
                file,
                event -> fail("No event expected")
        );

        assertEquals(150, report.totalLines());
        assertEquals(150, report.rejected());
        assertEquals(
                SuricataJsonlImporter.MAX_REPORTED_ERRORS,
                report.errors().size()
        );
    }

    @Test
    void shouldSupportCrLfLineEndings()
            throws IOException {

        Path file = tempDir.resolve("windows.jsonl");

        Files.writeString(
                file,
                flow(80) + "\r\n"
                        + flow(443) + "\r\n"
        );

        ImportReport report = importer.importFile(
                file,
                event -> {}
        );

        assertEquals(2, report.totalLines());
        assertEquals(2, report.accepted());
        assertEquals(0, report.rejected());
    }
}
