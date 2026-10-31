package dev.kayquemiguel.aegis.ingestion.suricata;

import java.util.List;

public record ImportReport(
        long totalLines,
        long accepted,
        long ignored,
        long rejected,
        List<RejectedLine> errors
) {
    public ImportReport {
        errors = List.copyOf(errors);

        if (totalLines != accepted + ignored + rejected) {
            throw new IllegalArgumentException(
                    "Import counters are inconsistent"
            );
        }
    }

    public record RejectedLine(
            long lineNumber,
            String reason
    ) {
    }
}
