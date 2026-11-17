package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.pipeline.AuthenticationDetectionPipeline;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import dev.kayquemiguel.aegis.security.SensorAccessPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sensors")
public class AuthenticationEventController {

    private final AuthenticationDetectionPipeline pipeline;
    private final SensorAccessPolicy accessPolicy;

    public AuthenticationEventController(
            AuthenticationDetectionPipeline pipeline,
            SensorAccessPolicy accessPolicy
    ) {
        this.pipeline = pipeline;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping("/{sensorId}/auth-events")
    public ResponseEntity<IngestionResponse> ingest(
            @PathVariable String sensorId,
            @RequestBody AuthenticationEventRequest request,
            Authentication authentication
    ) {
        accessPolicy.requireRead(authentication, sensorId);

        if (request == null
                || request.id() == null
                || request.occurredAt() == null
                || request.outcome() == null) {
            return ResponseEntity.badRequest().build();
        }

        Instant ingestedAt = Instant.now();

        if (request.occurredAt().isAfter(
                    ingestedAt.plusSeconds(300))
                || request.occurredAt().isBefore(
                    ingestedAt.minusSeconds(86400))) {
            return ResponseEntity.badRequest().build();
        }

        AuthenticationEvent event;

        try {
            event = new AuthenticationEvent(
                    request.id(),
                    request.occurredAt(),
                    ingestedAt,
                    request.sourceIp(),
                    request.username(),
                    request.sourceSystem(),
                    sensorId,
                    request.outcome()
            );
        } catch (IllegalArgumentException
                 | NullPointerException exception) {
            return ResponseEntity.badRequest().build();
        }

        var alertId = pipeline.process(event);

        IngestionResponse response = new IngestionResponse(
                alertId.isPresent(),
                alertId.orElse(null)
        );

        return ResponseEntity
                .status(alertId.isPresent()
                        ? HttpStatus.CREATED
                        : HttpStatus.ACCEPTED)
                .body(response);
    }

    public record AuthenticationEventRequest(
            UUID id,
            Instant occurredAt,
            String sourceIp,
            String username,
            String sourceSystem,
            AuthenticationOutcome outcome
    ) {
    }

    public record IngestionResponse(
            boolean alertGenerated,
            UUID alertId
    ) {
    }
}
