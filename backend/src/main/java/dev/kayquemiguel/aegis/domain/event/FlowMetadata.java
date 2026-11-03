
package dev.kayquemiguel.aegis.domain.event;

public record FlowMetadata(
        String state,
        String reason
) {

    public FlowMetadata {
        state = normalize(state, "state");
        reason = normalize(reason, "reason");

        if (state == null && reason == null) {
            throw new IllegalArgumentException(
                    "Flow metadata must contain at least one value"
            );
        }
    }

    private static String normalize(
            String value,
            String field
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        if (normalized.length() > 128) {
            throw new IllegalArgumentException(
                    field + " exceeds maximum length"
            );
        }

        return normalized;
    }
}
