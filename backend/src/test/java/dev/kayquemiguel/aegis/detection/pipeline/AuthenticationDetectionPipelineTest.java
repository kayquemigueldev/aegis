package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthenticationDetectionPipelineTest {

    private static final Instant BASE =
            Instant.parse("2026-11-16T10:00:00Z");

    @Autowired
    private AuthenticationDetectionPipeline pipeline;

    @Autowired
    private DetectionAlertRepository repository;

    private AuthenticationEvent event(
            String sensorId,
            String username,
            AuthenticationOutcome outcome,
            int seconds
    ) {
        return new AuthenticationEvent(
                UUID.randomUUID(),
                BASE.plusSeconds(seconds),
                BASE.plusSeconds(seconds + 1),
                "192.0.2.10",
                username,
                "auth-log",
                sensorId,
                outcome
        );
    }

    @Test
    void shouldPersistAlertAfterFiveFailures() {
        String sensor = "test-" + UUID.randomUUID();

        for (int i = 0; i < 4; i++) {
            assertTrue(pipeline.process(
                    event(
                            sensor,
                            "admin",
                            AuthenticationOutcome.FAILURE,
                            i
                    )
            ).isEmpty());
        }

        UUID id = pipeline.process(
                event(
                        sensor,
                        "admin",
                        AuthenticationOutcome.FAILURE,
                        4
                )
        ).orElseThrow();

        repository.flush();

        var saved = repository.findById(id).orElseThrow();

        assertEquals("NET-003", saved.getRuleId());
        assertEquals(sensor, saved.getSensorId());
        assertEquals("admin", saved.getAuthUsername());
        assertEquals(5, saved.getObservedCount());
        assertEquals(
                "AUTHENTICATION_FAILURES",
                saved.getMetricType()
        );
    }

    @Test
    void shouldIgnoreSuccessfulAuthentications() {
        String sensor = "test-" + UUID.randomUUID();

        for (int i = 0; i < 4; i++) {
            assertTrue(pipeline.process(
                    event(
                            sensor,
                            "admin",
                            AuthenticationOutcome.FAILURE,
                            i
                    )
            ).isEmpty());
        }

        assertTrue(pipeline.process(
                event(
                        sensor,
                        "admin",
                        AuthenticationOutcome.SUCCESS,
                        4
                )
        ).isEmpty());

        assertTrue(pipeline.process(
                event(
                        sensor,
                        "admin",
                        AuthenticationOutcome.FAILURE,
                        5
                )
        ).isPresent());
    }

    @Test
    void shouldKeepUsernamesIsolated() {
        String sensor = "test-" + UUID.randomUUID();

        for (int i = 0; i < 4; i++) {
            pipeline.process(event(
                    sensor,
                    "admin",
                    AuthenticationOutcome.FAILURE,
                    i
            ));
        }

        assertTrue(pipeline.process(
                event(
                        sensor,
                        "guest",
                        AuthenticationOutcome.FAILURE,
                        4
                )
        ).isEmpty());

        assertTrue(pipeline.process(
                event(
                        sensor,
                        "admin",
                        AuthenticationOutcome.FAILURE,
                        5
                )
        ).isPresent());
    }
}
