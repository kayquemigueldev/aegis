package dev.kayquemiguel.aegis.detection.persistence;

public final class AuthenticationEventLimitException
        extends RuntimeException {

    public AuthenticationEventLimitException(
            String message
    ) {
        super(message);
    }
}
