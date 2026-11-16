package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HorizontalPortScanDetectorTest {

    private static final Instant BASE =
            Instant.parse("2026-11-16T12:00:00Z");

    private final HorizontalPortScanDetector detector =
            new HorizontalPortScanDetector(
                    Duration.ofSeconds(60), 3
            );

    private static NetworkEvent event(
            String sensor,
            String source,
            String destination,
            int port,
            String flowId,
            long seconds,
            NetworkProtocol protocol
    ) {
        Instant timestamp = BASE.plusSeconds(seconds);

        return new NetworkEvent(
                UUID.randomUUID(),
                timestamp,
                timestamp.plusSeconds(1),
                source,
                destination,
                49152,
                port,
                protocol,
                "suricata",
                flowId,
                null,
                sensor
        );
    }

    private static NetworkEvent tcp(
            String destination,
            int port,
            String flowId,
            long seconds
    ) {
        return event(
                "sensor-001",
                "192.0.2.10",
                destination,
                port,
                flowId,
                seconds,
                NetworkProtocol.TCP
        );
    }

    @Test
    void shouldDetectHorizontalPortScan() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "101", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "102", 10)
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp("192.0.2.22", 22, "103", 20)
        ).orElseThrow();

        assertEquals("NET-002", alert.ruleId());
        assertEquals("sensor-001", alert.sensorId());
        assertEquals("192.0.2.10", alert.sourceIp());
        assertEquals(3, alert.threshold());
    }

    @Test
    void shouldIgnoreRepeatedDestination() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "201", 0)
        );

        detector.evaluate(
                tcp("192.0.2.20", 22, "202", 10)
        );

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "203", 20)
        ).isEmpty());
    }

    @Test
    void shouldNotMixDifferentPorts() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "301", 0)
        );

        detector.evaluate(
                tcp("192.0.2.21", 80, "302", 10)
        );

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "303", 20)
        ).isEmpty());
    }

    @Test
    void shouldNotMixSensors() {
        detector.evaluate(event(
                "sensor-001",
                "192.0.2.10",
                "192.0.2.20",
                22, "401", 0,
                NetworkProtocol.TCP
        ));

        detector.evaluate(event(
                "sensor-002",
                "192.0.2.10",
                "192.0.2.21",
                22, "402", 10,
                NetworkProtocol.TCP
        ));

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "403", 20)
        ).isEmpty());
    }

    @Test
    void shouldNotMixSourceIps() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "501", 0)
        );

        detector.evaluate(event(
                "sensor-001",
                "192.0.2.30",
                "192.0.2.21",
                22, "502", 10,
                NetworkProtocol.TCP
        ));

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "503", 20)
        ).isEmpty());
    }

    @Test
    void shouldIgnoreUdpTraffic() {
        for (int i = 0; i < 3; i++) {
            assertTrue(detector.evaluate(event(
                    "sensor-001",
                    "192.0.2.10",
                    "192.0.2." + (20 + i),
                    22,
                    "udp-" + i,
                    i * 10,
                    NetworkProtocol.UDP
            )).isEmpty());
        }
    }

    @Test
    void shouldExpireOldObservations() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "601", 0)
        );

        detector.evaluate(
                tcp("192.0.2.21", 22, "602", 10)
        );

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "603", 90)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.23", 22, "604", 95)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.24", 22, "605", 100)
        ).isPresent());
    }

    @Test
    void shouldDeduplicateFlowId() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "701", 0)
        );

        detector.evaluate(
                tcp("192.0.2.21", 22, "701", 10)
        );

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "702", 20)
        ).isEmpty());
    }

    @Test
    void shouldNotRepeatAlertAboveThreshold() {
        detector.evaluate(
                tcp("192.0.2.20", 22, "801", 0)
        );

        detector.evaluate(
                tcp("192.0.2.21", 22, "802", 10)
        );

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "803", 20)
        ).isPresent());

        assertTrue(detector.evaluate(
                tcp("192.0.2.23", 22, "804", 30)
        ).isEmpty());
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HorizontalPortScanDetector(
                        Duration.ZERO, 3
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new HorizontalPortScanDetector(
                        Duration.ofSeconds(60), 1
                )
        );
    }

    @Test
    void shouldAcceptSlightlyLateHorizontalScanEvent() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "late-h-1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "late-h-2", 30)
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp("192.0.2.22", 22, "late-h-3", 20)
        ).orElseThrow();

        assertEquals("NET-002", alert.ruleId());
        assertEquals(3, alert.observedCount());
        assertEquals(
                DetectionMetricType.DISTINCT_DESTINATION_HOSTS,
                alert.metricType()
        );
    }


    @Test
    void shouldRejectHorizontalEventBeyondLatenessLimit() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "late-limit-h-1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "late-limit-h-2", 30)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "late-limit-h-3", 19)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.23", 22, "late-limit-h-4", 40)
        ).isPresent());
    }

    @Test
    void shouldDeduplicateSlightlyLateHorizontalFlow() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "duplicate-h-1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "duplicate-h-2", 30)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "duplicate-h-1", 20)
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp("192.0.2.23", 22, "duplicate-h-3", 40)
        ).orElseThrow();

        assertEquals(3, alert.observedCount());
    }


    @Test
    void shouldExpireLateHorizontalObservationsAndRecover() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "window-h-1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "window-h-2", 30)
        ).isEmpty());

        DetectionAlert first = detector.evaluate(
                tcp("192.0.2.22", 22, "window-h-3", 20)
        ).orElseThrow();

        assertEquals(3, first.observedCount());

        assertTrue(detector.evaluate(
                tcp("192.0.2.23", 22, "window-h-4", 90)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.24", 22, "window-h-5", 95)
        ).isEmpty());

        DetectionAlert second = detector.evaluate(
                tcp("192.0.2.25", 22, "window-h-6", 100)
        ).orElseThrow();

        assertEquals("NET-002", second.ruleId());
        assertEquals(3, second.observedCount());
    }


    @Test
    void shouldAllowReusedHorizontalFlowIdAfterExpiration() {
        assertTrue(detector.evaluate(
                tcp("192.0.2.20", 22, "reused-h", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.21", 22, "reused-h", 70)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp("192.0.2.22", 22, "reused-h-2", 80)
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp("192.0.2.23", 22, "reused-h-3", 90)
        ).orElseThrow();

        assertEquals("NET-002", alert.ruleId());
        assertEquals(3, alert.observedCount());
    }


    @Test
    void shouldEvictOldestHorizontalFlowAtCapacity() {
        for (int i = 0; i < 512; i++) {
            assertTrue(detector.evaluate(
                    tcp(
                            "192.0.2.20",
                            22,
                            "capacity-h-" + i,
                            0
                    )
            ).isEmpty());
        }

        // A observação 513 força a remoção da primeira.
        assertTrue(detector.evaluate(
                tcp(
                        "192.0.2.20",
                        22,
                        "capacity-h-512",
                        1
                )
        ).isEmpty());

        // Agora podemos reutilizar o flowId removido.
        assertTrue(detector.evaluate(
                tcp(
                        "192.0.2.21",
                        22,
                        "capacity-h-0",
                        2
                )
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(
                        "192.0.2.22",
                        22,
                        "capacity-h-new",
                        3
                )
        ).orElseThrow();

        assertEquals("NET-002", alert.ruleId());
        assertEquals(3, alert.observedCount());
    }

}
