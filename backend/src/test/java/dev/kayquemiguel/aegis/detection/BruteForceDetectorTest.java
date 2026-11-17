package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.AuthenticationEvent;
import dev.kayquemiguel.aegis.domain.event.AuthenticationOutcome;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceDetectorTest {

    private static final Instant BASE =
            Instant.parse("2026-11-16T10:00:00Z");

    private final BruteForceDetector detector =
            new BruteForceDetector(Duration.ofSeconds(60), 5);

    private AuthenticationEvent event(
            String id,
            long seconds,
            String username,
            AuthenticationOutcome outcome
    ) {
        return new AuthenticationEvent(
                UUID.nameUUIDFromBytes(
                        id.getBytes(StandardCharsets.UTF_8)
                ),
                BASE.plusSeconds(seconds),
                BASE.plusSeconds(seconds + 1),
                "192.0.2.10",
                username,
                "auth-log",
                "sensor-001",
                outcome
        );
    }

    private AuthenticationEvent failure(
            String id,
            long seconds
    ) {
        return event(
                id,
                seconds,
                "admin",
                AuthenticationOutcome.FAILURE
        );
    }

    @Test
    void shouldAlertAfterFiveFailures() {
        for (int i = 0; i < 4; i++) {
            assertTrue(detector.evaluate(
                    failure("failure-" + i, i * 10)
            ).isEmpty());
        }

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("failure-4", 40)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());
        assertEquals(5, alert.threshold());
        assertEquals("admin", alert.username());
        assertEquals("sensor-001", alert.sensorId());
    }

    @Test
    void shouldIgnoreSuccessfulLogins() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(failure("auth-f-" + i, i));
        }

        assertTrue(detector.evaluate(
                event(
                        "auth-success",
                        5,
                        "admin",
                        AuthenticationOutcome.SUCCESS
                )
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("auth-f-4", 6)
        ).isPresent());
    }

    @Test
    void shouldDeduplicateAuthenticationEventIds() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(failure("dup-" + i, i));
        }

        assertTrue(detector.evaluate(
                failure("dup-0", 5)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("dup-4", 6)
        ).isPresent());
    }

    @Test
    void shouldIsolateDifferentUsernames() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(failure("admin-" + i, i));
        }

        assertTrue(detector.evaluate(
                event(
                        "other-user",
                        5,
                        "guest",
                        AuthenticationOutcome.FAILURE
                )
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("admin-4", 6)
        ).isPresent());

        assertEquals(2, detector.trackedGroupCount());
    }

    @Test
    void shouldExpireFailuresOutsideWindow() {
        detector.evaluate(failure("old-1", 0));
        detector.evaluate(failure("old-2", 10));
        detector.evaluate(failure("old-3", 20));
        detector.evaluate(failure("old-4", 30));

        assertTrue(detector.evaluate(
                failure("new-1", 70)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("new-2", 80)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("new-3", 90)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("new-4", 100)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("new-5", 110)
        ).isPresent());
    }

    @Test
    void shouldAlertOnlyOnThresholdCrossing() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(failure("cross-" + i, i));
        }

        assertTrue(detector.evaluate(
                failure("cross-4", 4)
        ).isPresent());

        assertTrue(detector.evaluate(
                failure("cross-5", 5)
        ).isEmpty());
    }

    private AuthenticationEvent customEvent(
            String id,
            long seconds,
            String sourceIp,
            String sourceSystem,
            String sensorId
    ) {
        return new AuthenticationEvent(
                UUID.nameUUIDFromBytes(
                        id.getBytes(StandardCharsets.UTF_8)
                ),
                BASE.plusSeconds(seconds),
                BASE.plusSeconds(seconds + 1),
                sourceIp,
                "admin",
                sourceSystem,
                sensorId,
                AuthenticationOutcome.FAILURE
        );
    }

    @Test
    void shouldIsolateSourceIps() {
        for (int i = 0; i < 4; i++) {
            assertTrue(detector.evaluate(
                    failure("ip-primary-" + i, i)
            ).isEmpty());
        }

        assertTrue(detector.evaluate(
                customEvent(
                        "ip-other",
                        5,
                        "192.0.2.11",
                        "auth-log",
                        "sensor-001"
                )
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("ip-primary-4", 6)
        ).isPresent());

        assertEquals(2, detector.trackedGroupCount());
    }

    @Test
    void shouldIsolateSensors() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(
                    failure("sensor-primary-" + i, i)
            );
        }

        assertTrue(detector.evaluate(
                customEvent(
                        "sensor-other",
                        5,
                        "192.0.2.10",
                        "auth-log",
                        "sensor-002"
                )
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("sensor-primary-4", 6)
        ).isPresent());

        assertEquals(2, detector.trackedGroupCount());
    }

    @Test
    void shouldIsolateSourceSystems() {
        for (int i = 0; i < 4; i++) {
            detector.evaluate(
                    failure("system-primary-" + i, i)
            );
        }

        assertTrue(detector.evaluate(
                customEvent(
                        "system-other",
                        5,
                        "192.0.2.10",
                        "ssh-auth",
                        "sensor-001"
                )
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("system-primary-4", 6)
        ).isPresent());

        assertEquals(2, detector.trackedGroupCount());
    }

    @Test
    void shouldAcceptOutOfOrderFailuresWithinLateness() {
        assertTrue(detector.evaluate(
                failure("late-0", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("late-1", 10)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("late-2", 20)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("late-3", 30)
        ).isEmpty());

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("late-4", 25)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());
        assertEquals(BASE.plusSeconds(30), alert.detectedAt());
    }

    @Test
    void shouldRejectEventsBeyondAllowedLateness() {
        assertTrue(detector.evaluate(
                failure("too-late-start", 70)
        ).isEmpty());

        // Mais de 10 segundos atrasado.
        assertTrue(detector.evaluate(
                failure("too-late-rejected", 50)
        ).isEmpty());

        for (int i = 0; i < 3; i++) {
            assertTrue(detector.evaluate(
                    failure("too-late-new-" + i, 80 + i)
            ).isEmpty());
        }

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("too-late-final", 83)
                ).orElseThrow();

        assertEquals(5, alert.observedCount());
    }


    @Test
    void shouldExpireInactiveAuthenticationGroups() {
        assertTrue(detector.evaluate(
                failure("idle-original", 0)
        ).isEmpty());

        assertEquals(1, detector.trackedGroupCount());

        assertTrue(detector.evaluate(
                customEvent(
                        "idle-second",
                        601,
                        "192.0.2.11",
                        "auth-log",
                        "sensor-001"
                )
        ).isEmpty());

        // O grupo original ficou inativo por mais de 10 min.
        assertEquals(1, detector.trackedGroupCount());

        assertTrue(detector.evaluate(
                failure("idle-return", 602)
        ).isEmpty());

        assertEquals(2, detector.trackedGroupCount());
    }

    @Test
    void shouldReuseEventIdAfterWindowExpiration() {
        assertTrue(detector.evaluate(
                failure("reused-id", 0)
        ).isEmpty());

        // A observacao original sai da janela de 60 s.
        assertTrue(detector.evaluate(
                failure("reused-id", 70)
        ).isEmpty());

        for (int i = 1; i <= 3; i++) {
            assertTrue(detector.evaluate(
                    failure("reuse-new-" + i, 70 + i)
            ).isEmpty());
        }

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("reuse-final", 74)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());
    }

    @Test
    void shouldEvictOldestGroupAtCapacity() {
        assertTrue(detector.evaluate(
                failure("capacity-original", 0)
        ).isEmpty());

        // Cada IP cria um grupo independente.
        for (int i = 1; i <= 9999; i++) {
            String ip = "10.0."
                    + (i / 256)
                    + "."
                    + (i % 256);

            assertTrue(detector.evaluate(
                    customEvent(
                            "capacity-" + i,
                            0,
                            ip,
                            "auth-log",
                            "sensor-001"
                    )
            ).isEmpty());
        }

        assertEquals(
                10_000,
                detector.trackedGroupCount()
        );

        // O grupo 10.001 remove o primeiro grupo (LRU).
        assertTrue(detector.evaluate(
                customEvent(
                        "capacity-overflow",
                        1,
                        "10.40.0.1",
                        "auth-log",
                        "sensor-001"
                )
        ).isEmpty());

        assertEquals(
                10_000,
                detector.trackedGroupCount()
        );

        // A origem original precisa recomecar do zero.
        for (int i = 0; i < 4; i++) {
            assertTrue(detector.evaluate(
                    failure("capacity-return-" + i, i + 2)
            ).isEmpty());
        }

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("capacity-return-final", 6)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());

        assertEquals(
                10_000,
                detector.trackedGroupCount()
        );
    }


    @Test
    void shouldBoundObservationsDuringSustainedFailures() {
        int alerts = 0;

        for (int i = 0; i < 1024; i++) {
            if (detector.evaluate(
                    failure("bounded-" + i, 0)
            ).isPresent()) {
                alerts++;
            }
        }

        // Somente um alerta ao cruzar o limite.
        assertEquals(1, alerts);

        assertEquals(
                512,
                detector.trackedObservationCount(
                        "sensor-001",
                        "auth-log",
                        "192.0.2.10",
                        "admin"
                )
        );

        // A janela anterior expirou.
        for (int i = 0; i < 4; i++) {
            assertTrue(detector.evaluate(
                    failure("bounded-new-" + i, 100 + i)
            ).isEmpty());
        }

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("bounded-new-4", 104)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());

        assertEquals(
                5,
                detector.trackedObservationCount(
                        "sensor-001",
                        "auth-log",
                        "192.0.2.10",
                        "admin"
                )
        );
    }

    @Test
    void shouldAcceptEventExactlyAtLatenessBoundary() {
        assertTrue(detector.evaluate(
                failure("boundary-20", 20)
        ).isEmpty());

        // Exatamente 10 segundos atrasado: permitido.
        assertTrue(detector.evaluate(
                failure("boundary-10", 10)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("boundary-21", 21)
        ).isEmpty());

        assertTrue(detector.evaluate(
                failure("boundary-22", 22)
        ).isEmpty());

        AuthenticationDetectionAlert alert =
                detector.evaluate(
                        failure("boundary-23", 23)
                ).orElseThrow();

        assertEquals("NET-003", alert.ruleId());
        assertEquals(5, alert.observedCount());
        assertEquals(
                BASE.plusSeconds(23),
                alert.detectedAt()
        );
    }

}
