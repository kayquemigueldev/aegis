package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.BruteForceDetector;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationDetectionPipeline {

    private final BruteForceDetector detector =
            new BruteForceDetector(
                    Duration.ofSeconds(60),
                    5
            );

    private final DetectionAlertService alertService;

    public AuthenticationDetectionPipeline(
            DetectionAlertService alertService
    ) {
        this.alertService = alertService;
    }

    public synchronized Optional<UUID> process(
            AuthenticationEvent event
    ) {
        Objects.requireNonNull(
                event, "event is required"
        );

        return detector.evaluate(event)
                .map(alertService::saveAuthentication);
    }
}
