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

    private static final int MAX_REPORTED_ERRORS = 100;

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
                path, StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {
                total++;

                Instant ingestedAt = clock.instant();
                Optional<NetworkEvent> event;

                try {
                    event = parser.parse(line, ingestedAt);
                } catch (IllegalArgumentException exception) {
                    rejected++;

                    if (errors.size() < MAX_REPORTED_ERRORS) {
                        String reason = exception.getMessage();

                        errors.add(
                                new ImportReport.RejectedLine(
                                        total,
                                        reason == null
                                                ? "Invalid event"
                                                : reason
                                )
                        );
                    }
                    continue;
                }

                if (event.isEmpty()) {
                    ignored++;
                } else {
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
}
