package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
import dev.kayquemiguel.aegis.detection.BruteForceDetector;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationDetectionPipeline {

    private static final int MAX_PENDING_ALERTS = 1024;

    private final BruteForceDetector detector =
            new BruteForceDetector(
                    Duration.ofSeconds(60),
                    5
            );

    private final Map<PendingKey, AuthenticationDetectionAlert>
            pendingAlerts = new LinkedHashMap<>();

    private final DetectionAlertService alertService;

    public AuthenticationDetectionPipeline(
            DetectionAlertService alertService
    ) {
        this.alertService = Objects.requireNonNull(
                alertService,
                "alertService is required"
        );
    }

    public synchronized Optional<UUID> process(
            AuthenticationEvent event
    ) {
        Objects.requireNonNull(
                event, "event is required"
        );

        PendingKey key = PendingKey.from(event);

        AuthenticationDetectionAlert pending =
                pendingAlerts.get(key);

        if (pending != null) {
            return Optional.of(
                    persistPending(key, pending)
            );
        }

        // Nunca descartar um alerta pendente para
        // abrir espaco para eventos novos.
        if (pendingAlerts.size() >= MAX_PENDING_ALERTS) {
            throw new IllegalStateException(
                    "Authentication alert pending queue is full"
            );
        }

        Optional<AuthenticationDetectionAlert> detected =
                detector.evaluate(event);

        if (detected.isEmpty()) {
            return Optional.empty();
        }

        AuthenticationDetectionAlert alert =
                detected.orElseThrow();

        // Registrar antes de tentar a persistencia.
        pendingAlerts.put(key, alert);

        return Optional.of(
                persistPending(key, alert)
        );
    }

    private UUID persistPending(
            PendingKey key,
            AuthenticationDetectionAlert alert
    ) {
        // Caso o banco falhe, a excecao se propaga,
        // mas o alerta permanece em pendingAlerts.
        UUID id = alertService.saveAuthentication(alert);

        // Remover somente depois do retorno de sucesso.
        pendingAlerts.remove(key);

        return id;
    }

    private record PendingKey(
            String sensorId,
            String sourceSystem,
            String sourceIp,
            String username,
            UUID eventId
    ) {
        static PendingKey from(AuthenticationEvent event) {
            return new PendingKey(
                    event.sensorId(),
                    event.sourceSystem(),
                    event.sourceIp(),
                    event.username(),
                    event.id()
            );
        }
    }
}
