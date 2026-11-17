package dev.kayquemiguel.aegis.detection.pipeline;

import dev.kayquemiguel.aegis.detection.AuthenticationDetectionAlert;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationDetectionRecoveryTest {

    private static final Instant BASE =
            Instant.parse("2026-11-16T10:00:00Z");

    private AuthenticationEvent failure(
            UUID id,
            int seconds
    ) {
        return new AuthenticationEvent(
                id,
                BASE.plusSeconds(seconds),
                BASE.plusSeconds(seconds + 1),
                "192.0.2.10",
                "admin",
                "auth-log",
                "sensor-recovery",
                AuthenticationOutcome.FAILURE
        );
    }

    @Test
    void shouldRetryAlertAfterPersistenceFailure() {
        DetectionAlertService persistence =
                mock(DetectionAlertService.class);

        UUID expectedId = UUID.randomUUID();

        when(persistence.saveAuthentication(
                any(AuthenticationDetectionAlert.class)
        ))
                .thenThrow(
                        new IllegalStateException(
                                "Simulated database outage"
                        )
                )
                .thenReturn(expectedId);

        AuthenticationDetectionPipeline pipeline =
                new AuthenticationDetectionPipeline(
                        persistence
                );

        for (int i = 0; i < 4; i++) {
            assertTrue(
                    pipeline.process(
                            failure(UUID.randomUUID(), i)
                    ).isEmpty()
            );
        }

        UUID finalEventId = UUID.randomUUID();

        AuthenticationEvent fifth =
                failure(finalEventId, 4);

        assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(fifth)
        );

        // O reenvio deve recuperar o alerta pendente.
        assertEquals(
                expectedId,
                pipeline.process(fifth).orElseThrow()
        );

        verify(persistence, times(2))
                .saveAuthentication(
                        any(AuthenticationDetectionAlert.class)
                );
    }

    @Test
    void shouldRecoverAfterMultiplePersistenceFailures() {
        DetectionAlertService persistence =
                mock(DetectionAlertService.class);

        UUID expectedId = UUID.randomUUID();

        when(persistence.saveAuthentication(
                any(AuthenticationDetectionAlert.class)
        ))
                .thenThrow(new IllegalStateException("DB failure 1"))
                .thenThrow(new IllegalStateException("DB failure 2"))
                .thenReturn(expectedId);

        AuthenticationDetectionPipeline pipeline =
                new AuthenticationDetectionPipeline(persistence);

        for (int i = 0; i < 4; i++) {
            assertTrue(pipeline.process(
                    failure(UUID.randomUUID(), i)
            ).isEmpty());
        }

        AuthenticationEvent fifth =
                failure(UUID.randomUUID(), 4);

        assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(fifth)
        );

        assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(fifth)
        );

        assertEquals(
                expectedId,
                pipeline.process(fifth).orElseThrow()
        );

        verify(persistence, times(3))
                .saveAuthentication(
                        any(AuthenticationDetectionAlert.class)
                );

        assertTrue(pipeline.process(fifth).isEmpty());
    }


    @Test
    void shouldIsolatePendingAlertsBySensor() {
        DetectionAlertService persistence =
                mock(DetectionAlertService.class);

        when(persistence.saveAuthentication(
                any(AuthenticationDetectionAlert.class)
        ))
                .thenThrow(
                        new IllegalStateException("DB unavailable")
                );

        AuthenticationDetectionPipeline pipeline =
                new AuthenticationDetectionPipeline(persistence);

        for (int i = 0; i < 4; i++) {
            pipeline.process(
                    failure(UUID.randomUUID(), i)
            );
        }

        UUID sharedId = UUID.randomUUID();

        AuthenticationEvent original =
                failure(sharedId, 4);

        assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(original)
        );

        AuthenticationEvent otherSensor =
                new AuthenticationEvent(
                        sharedId,
                        BASE.plusSeconds(4),
                        BASE.plusSeconds(5),
                        "192.0.2.10",
                        "admin",
                        "auth-log",
                        "sensor-other",
                        AuthenticationOutcome.FAILURE
                );

        assertTrue(
                pipeline.process(otherSensor).isEmpty()
        );

        verify(persistence, times(1))
                .saveAuthentication(
                        any(AuthenticationDetectionAlert.class)
                );
    }

    @Test
    void shouldRejectNewEventsWhenPendingQueueIsFull() {
        DetectionAlertService persistence =
                mock(DetectionAlertService.class);

        when(persistence.saveAuthentication(
                any(AuthenticationDetectionAlert.class)
        )).thenThrow(
                new IllegalStateException("Database unavailable")
        );

        AuthenticationDetectionPipeline pipeline =
                new AuthenticationDetectionPipeline(persistence);

        AuthenticationEvent firstPending = null;

        for (int group = 0; group < 1024; group++) {
            String sensorId = "sensor-" + group;

            for (int i = 0; i < 4; i++) {
                AuthenticationEvent event =
                        new AuthenticationEvent(
                                UUID.randomUUID(),
                                BASE.plusSeconds(i),
                                BASE.plusSeconds(i + 1),
                                "192.0.2.10",
                                "admin",
                                "auth-log",
                                sensorId,
                                AuthenticationOutcome.FAILURE
                        );

                assertTrue(pipeline.process(event).isEmpty());
            }

            AuthenticationEvent fifth =
                    new AuthenticationEvent(
                            UUID.randomUUID(),
                            BASE.plusSeconds(4),
                            BASE.plusSeconds(5),
                            "192.0.2.10",
                            "admin",
                            "auth-log",
                            sensorId,
                            AuthenticationOutcome.FAILURE
                    );

            if (group == 0) {
                firstPending = fifth;
            }

            assertThrows(
                    IllegalStateException.class,
                    () -> pipeline.process(fifth)
            );
        }

        AuthenticationEvent overflow =
                new AuthenticationEvent(
                        UUID.randomUUID(),
                        BASE.plusSeconds(6),
                        BASE.plusSeconds(7),
                        "192.0.2.11",
                        "admin",
                        "auth-log",
                        "sensor-overflow",
                        AuthenticationOutcome.FAILURE
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(overflow)
        );

        assertEquals(
                "Authentication alert pending queue is full",
                exception.getMessage()
        );

        // Um alerta existente continua recuperavel,
        // mesmo com a fila totalmente ocupada.
        AuthenticationEvent retry = firstPending;

        assertNotNull(retry);

        assertThrows(
                IllegalStateException.class,
                () -> pipeline.process(retry)
        );

        verify(persistence, times(1025))
                .saveAuthentication(
                        any(AuthenticationDetectionAlert.class)
                );
    }

}
