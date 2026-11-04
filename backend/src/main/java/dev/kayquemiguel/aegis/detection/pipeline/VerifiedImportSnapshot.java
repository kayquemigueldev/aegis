
package dev.kayquemiguel.aegis.detection.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class VerifiedImportSnapshot
        implements AutoCloseable {

    public static final long MAX_FILE_BYTES =
            32L * 1024 * 1024;

    private final Path source;
    private final Path snapshot;
    private final String sha256;

    private VerifiedImportSnapshot(
            Path source,
            Path snapshot,
            String sha256
    ) {
        this.source = source;
        this.snapshot = snapshot;
        this.sha256 = sha256;
    }

    public static VerifiedImportSnapshot create(
            Path source
    ) throws IOException {
        Objects.requireNonNull(
                source, "source is required"
        );

        if (!Files.isRegularFile(source)) {
            throw new IOException(
                    "Input must be a regular file"
            );
        }

        Path normalized = source.toAbsolutePath()
                .normalize();

        Path temporary = Files.createTempFile(
                "aegis-import-",
                ".jsonl"
        );

        try {
            MessageDigest digest = newDigest();

            long totalBytes = 0;
            byte[] buffer = new byte[8192];

            try (
                    InputStream input =
                            Files.newInputStream(normalized);
                    OutputStream output =
                            Files.newOutputStream(temporary)
            ) {
                int read;

                while ((read = input.read(buffer)) != -1) {
                    if (read == 0) {
                        continue;
                    }

                    totalBytes += read;

                    if (totalBytes > MAX_FILE_BYTES) {
                        throw new IOException(
                                "Import file exceeds 32 MiB limit"
                        );
                    }

                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }

            return new VerifiedImportSnapshot(
                    normalized,
                    temporary,
                    HexFormat.of().formatHex(
                            digest.digest()
                    )
            );

        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }

            throw exception;
        }
    }

    public Path path() {
        return snapshot;
    }

    public String sha256() {
        return sha256;
    }

    public void verifySourceUnchanged()
            throws IOException {

        MessageDigest digest = newDigest();

        byte[] buffer = new byte[8192];
        long totalBytes = 0;

        try (InputStream input =
                     Files.newInputStream(source)) {

            int read;

            while ((read = input.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }

                totalBytes += read;

                if (totalBytes > MAX_FILE_BYTES) {
                    throw new IOException(
                            "Source changed during import"
                    );
                }

                digest.update(buffer, 0, read);
            }

        } catch (IOException exception) {
            throw new IOException(
                    "Could not verify source integrity",
                    exception
            );
        }

        String currentHash =
                HexFormat.of().formatHex(digest.digest());

        if (!sha256.equals(currentHash)) {
            throw new IOException(
                    "Source changed during import"
            );
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(
                    "SHA-256"
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(snapshot);
    }
}
