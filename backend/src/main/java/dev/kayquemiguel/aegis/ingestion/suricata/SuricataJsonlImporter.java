
package dev.kayquemiguel.aegis.ingestion.suricata;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class SuricataJsonlImporter {

    public static final int MAX_LINE_LENGTH = 65_536;
    public static final int MAX_REPORTED_ERRORS = 100;

    private final SuricataFlowParser parser;
    private final Clock clock;

    public SuricataJsonlImporter() {
        this(new SuricataFlowParser(), Clock.systemUTC());
    }

    public SuricataJsonlImporter(
            SuricataFlowParser parser,
            Clock clock
    ) {
        this.parser = Objects.requireNonNull(parser);
        this.clock = Objects.requireNonNull(clock);
    }

    public ImportReport importFile(
            Path path,
            Consumer<NetworkEvent> consumer
    ) throws IOException {

        Objects.requireNonNull(path, "path is required");
        Objects.requireNonNull(consumer, "consumer is required");

        long total = 0;
        long accepted = 0;
        long ignored = 0;
        long rejected = 0;

        List<ImportReport.RejectedLine> errors =
                new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        )) {

            LineResult line;

            while ((line = readLimitedLine(reader)) != null) {
                total++;

                if (line.tooLong()) {
                    rejected++;
                    addError(
                            errors,
                            total,
                            "Line exceeds maximum length of "
                                    + MAX_LINE_LENGTH + " characters"
                    );
                    continue;
                }

                Instant ingestedAt = clock.instant();
                Optional<NetworkEvent> event;

                try {
                    event = parser.parse(
                            line.content(),
                            ingestedAt
                    );
                } catch (IllegalArgumentException exception) {
                    rejected++;
                    addError(
                            errors,
                            total,
                            exception.getMessage()
                    );
                    continue;
                }

                if (event.isEmpty()) {
                    ignored++;
                } else {
                    // Falhas do consumidor nao sao erros de JSON.
                    consumer.accept(event.get());
                    accepted++;
                }
            }
        }

        return new ImportReport(
                total,
                accepted,
                ignored,
                rejected,
                errors
        );
    }

    private static void addError(
            List<ImportReport.RejectedLine> errors,
            long lineNumber,
            String reason
    ) {
        if (errors.size() >= MAX_REPORTED_ERRORS) {
            return;
        }

        errors.add(
                new ImportReport.RejectedLine(
                        lineNumber,
                        reason == null ? "Invalid event" : reason
                )
        );
    }

    private static LineResult readLimitedLine(
            BufferedReader reader
    ) throws IOException {

        StringBuilder content = new StringBuilder();
        boolean tooLong = false;
        boolean hasData = false;

        int next;

        while ((next = reader.read()) != -1) {
            hasData = true;

            if (next == '\n') {
                break;
            }

            if (content.length() < MAX_LINE_LENGTH) {
                content.append((char) next);
            } else {
                tooLong = true;
            }
        }

        if (!hasData) {
            return null;
        }

        if (tooLong) {
            return new LineResult("", true);
        }

        // Suporte a terminadores CRLF.
        int length = content.length();

        if (length > 0 && content.charAt(length - 1) == '\r') {
            content.setLength(length - 1);
        }

        return new LineResult(content.toString(), false);
    }

    private record LineResult(
            String content,
            boolean tooLong
    ) {
    }
}
