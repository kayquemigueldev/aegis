
package dev.kayquemiguel.aegis.detection.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

class VerifiedImportSnapshotTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateExactSnapshotAndDeleteIt()
            throws IOException {

        Path original =
                tempDir.resolve("events.jsonl");

        String content =
                "{\"event_type\":\"flow\"}\n";

        Files.writeString(original, content);

        Path temporary;

        try (VerifiedImportSnapshot snapshot =
                     VerifiedImportSnapshot.create(original)) {

            temporary = snapshot.path();

            assertTrue(Files.exists(temporary));
            assertEquals(
                    content,
                    Files.readString(temporary)
            );

            assertEquals(
                    64,
                    snapshot.sha256().length()
            );

            assertDoesNotThrow(
                    snapshot::verifySourceUnchanged
            );
        }

        assertFalse(Files.exists(temporary));
        assertTrue(Files.exists(original));
    }

    @Test
    void shouldDetectModifiedSource()
            throws IOException {

        Path original =
                tempDir.resolve("mutable.jsonl");

        Files.writeString(
                original,
                "{\"event_type\":\"flow\"}\n"
        );

        try (VerifiedImportSnapshot snapshot =
                     VerifiedImportSnapshot.create(original)) {

            Files.writeString(
                    original,
                    "{\"event_type\":\"alert\"}\n"
            );

            IOException exception =
                    assertThrows(
                            IOException.class,
                            snapshot::verifySourceUnchanged
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("Source changed")
            );
        }
    }

    @Test
    void shouldKeepSnapshotUnchangedAfterSourceMutation()
            throws IOException {

        Path original =
                tempDir.resolve("source.jsonl");

        Files.writeString(original, "original\n");

        try (VerifiedImportSnapshot snapshot =
                     VerifiedImportSnapshot.create(original)) {

            Files.writeString(
                    original,
                    "changed\n",
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            assertEquals(
                    "original\n",
                    Files.readString(snapshot.path())
            );
        }
    }

    @Test
    void shouldRejectOversizedFile()
            throws IOException {

        Path original =
                tempDir.resolve("oversized.jsonl");

        try (var output =
                     Files.newOutputStream(original)) {

            byte[] chunk = new byte[8192];

            for (int i = 0; i < 4097; i++) {
                output.write(chunk);
            }
        }

        IOException exception =
                assertThrows(
                        IOException.class,
                        () -> VerifiedImportSnapshot.create(
                                original
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("32 MiB")
        );
    }

    @Test
    void shouldRejectMissingFile() {

        Path missing =
                tempDir.resolve("missing.jsonl");

        assertThrows(
                IOException.class,
                () -> VerifiedImportSnapshot.create(
                        missing
                )
        );
    }
}
