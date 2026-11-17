package dev.kayquemiguel.aegis.detection.persistence;

public final class AuthenticationEventConflictException
        extends RuntimeException {

    public AuthenticationEventConflictException(
            String message
    ) {
        super(message);
    }
}
