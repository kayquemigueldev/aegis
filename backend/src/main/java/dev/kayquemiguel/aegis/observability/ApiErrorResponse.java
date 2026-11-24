package dev.kayquemiguel.aegis.observability;

public record ApiErrorResponse(
        String code,
        String message,
        String correlationId
) {
}
