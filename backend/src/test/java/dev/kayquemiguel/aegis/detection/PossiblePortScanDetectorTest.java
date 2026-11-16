
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PossiblePortScanDetectorTest {

    private final PossiblePortScanDetector detector =
            new PossiblePortScanDetector(
                    Duration.ofSeconds(60), 3
            );

    private static NetworkEvent event(
            String source,
            String destination,
            int port,
            String flowId,
            long second,
            NetworkProtocol protocol
    ) {
        Instant timestamp =
                Instant.parse("2026-11-03T10:00:00Z")
                        .plusSeconds(second);

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
                flowId
        );
    }

    private static NetworkEvent tcp(
            int port,
            String flowId,
            long second
    ) {
        return event(
                "192.0.2.10",
                "192.0.2.20",
                port,
                flowId,
                second,
                NetworkProtocol.TCP
        );
    }

    @Test
    void shouldAlertWhenThresholdIsReached() {
        assertTrue(detector.evaluate(
                tcp(22, "1", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "2", 10)).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(443, "3", 20)
        ).orElseThrow();

        assertEquals("NET-001", alert.ruleId());
        assertEquals(3, alert.distinctPorts());
        assertEquals("192.0.2.10", alert.sourceIp());
        assertEquals("192.0.2.20", alert.destinationIp());
    }

    @Test
    void shouldNotAlertForRepeatedPort() {
        assertTrue(detector.evaluate(
                tcp(443, "1", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, "2", 10)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "3", 20)).isEmpty());
    }


    @Test
    void shouldDeduplicateSameFlowId() {
        assertTrue(detector.evaluate(
                tcp(22, "100", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "100", 10)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, "101", 20)).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(8080, "102", 30)
        ).orElseThrow();

        assertEquals("NET-001", alert.ruleId());
        assertEquals(3, alert.distinctPorts());

        assertTrue(detector.evaluate(
                tcp(8443, "103", 40)).isEmpty());
    }


    @Test
    void shouldNotMixDifferentSources() {
        detector.evaluate(tcp(22, "1", 0));

        detector.evaluate(event(
                "192.0.2.30",
                "192.0.2.20",
                80,
                "2",
                10,
                NetworkProtocol.TCP
        ));

        assertTrue(detector.evaluate(
                tcp(443, "3", 20)).isEmpty());
    }

    @Test
    void shouldIgnoreUdpEvents() {
        for (int i = 0; i < 5; i++) {
            NetworkEvent udp = event(
                    "192.0.2.10",
                    "192.0.2.20",
                    1000 + i,
                    String.valueOf(i),
                    i,
                    NetworkProtocol.UDP
            );

            assertTrue(detector.evaluate(udp).isEmpty());
        }
    }

    @Test
    void shouldExpireOldObservations() {
        detector.evaluate(tcp(22, "1", 0));
        detector.evaluate(tcp(80, "2", 10));

        assertTrue(detector.evaluate(
                tcp(443, "3", 90)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(8080, "4", 95)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(8443, "5", 100)).isPresent());
    }

    @Test
    void shouldNotEmitDuplicateAlertAboveThreshold() {
        detector.evaluate(tcp(22, "1", 0));
        detector.evaluate(tcp(80, "2", 10));

        assertTrue(detector.evaluate(
                tcp(443, "3", 20)).isPresent());

        assertTrue(detector.evaluate(
                tcp(8080, "4", 30)).isEmpty());
    }

    @Test
    void shouldIgnoreOutOfOrderEvents() {
        detector.evaluate(tcp(22, "1", 30));
        detector.evaluate(tcp(80, "2", 19));

        assertTrue(detector.evaluate(
                tcp(443, "3", 40)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(8080, "4", 50)).isPresent());
    }

    @Test
    void shouldIgnoreEventsWithoutFlowId() {
        assertTrue(detector.evaluate(
                tcp(22, null, 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, null, 10)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, null, 20)).isEmpty());
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PossiblePortScanDetector(
                        Duration.ZERO, 3
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PossiblePortScanDetector(
                        Duration.ofSeconds(60), 1
                )
        );
    }

    @Test
    void shouldAcceptSlightlyLateVerticalScanEvent() {
        assertTrue(detector.evaluate(
                tcp(22, "late-v-1", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "late-v-2", 30)).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(443, "late-v-3", 20)
        ).orElseThrow();

        assertEquals("NET-001", alert.ruleId());
        assertEquals(3, alert.observedCount());
        assertEquals(
                DetectionMetricType.DISTINCT_DESTINATION_PORTS,
                alert.metricType()
        );
    }


    @Test
    void shouldRejectVerticalEventBeyondLatenessLimit() {
        assertTrue(detector.evaluate(
                tcp(22, "late-limit-v-1", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "late-limit-v-2", 30)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, "late-limit-v-3", 19)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(8080, "late-limit-v-4", 40)).isPresent());
    }

    @Test
    void shouldDeduplicateSlightlyLateVerticalFlow() {
        assertTrue(detector.evaluate(
                tcp(22, "duplicate-v-1", 0)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "duplicate-v-2", 30)).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, "duplicate-v-1", 20)).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(8080, "duplicate-v-3", 40)
        ).orElseThrow();

        assertEquals(3, alert.observedCount());
    }


    @Test
    void shouldExpireLateVerticalObservationsAndRecover() {
        assertTrue(detector.evaluate(
                tcp(22, "window-v-1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "window-v-2", 30)
        ).isEmpty());

        DetectionAlert first = detector.evaluate(
                tcp(443, "window-v-3", 20)
        ).orElseThrow();

        assertEquals(3, first.observedCount());

        assertTrue(detector.evaluate(
                tcp(8080, "window-v-4", 90)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp(8443, "window-v-5", 95)
        ).isEmpty());

        DetectionAlert second = detector.evaluate(
                tcp(3306, "window-v-6", 100)
        ).orElseThrow();

        assertEquals("NET-001", second.ruleId());
        assertEquals(3, second.observedCount());
    }


    @Test
    void shouldAllowReusedVerticalFlowIdAfterExpiration() {
        assertTrue(detector.evaluate(
                tcp(22, "reused-v", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp(80, "reused-v", 70)
        ).isEmpty());

        assertTrue(detector.evaluate(
                tcp(443, "reused-v-2", 80)
        ).isEmpty());

        DetectionAlert alert = detector.evaluate(
                tcp(8080, "reused-v-3", 90)
        ).orElseThrow();

        assertEquals("NET-001", alert.ruleId());
        assertEquals(3, alert.observedCount());
    }


    @Test
    void shouldEvictOldestVerticalObservationAtCapacity() {
        PossiblePortScanDetector bounded =
                new PossiblePortScanDetector(
                        Duration.ofSeconds(60),
                        3,
                        Duration.ofMinutes(10),
                        2,
                        3
                );

        assertTrue(bounded.evaluate(
                tcp(22, "capacity-v-1", 0)
        ).isEmpty());

        assertTrue(bounded.evaluate(
                tcp(22, "capacity-v-2", 1)
        ).isEmpty());

        assertTrue(bounded.evaluate(
                tcp(22, "capacity-v-3", 2)
        ).isEmpty());

        assertEquals(
                3,
                bounded.trackedObservationCount(
                        "192.0.2.10",
                        "192.0.2.20"
                )
        );

        // Força a remoção do primeiro flowId.
        assertTrue(bounded.evaluate(
                tcp(22, "capacity-v-4", 3)
        ).isEmpty());

        // Agora o identificador antigo pode ser reutilizado.
        assertTrue(bounded.evaluate(
                tcp(80, "capacity-v-1", 4)
        ).isEmpty());

        DetectionAlert alert = bounded.evaluate(
                tcp(443, "capacity-v-5", 5)
        ).orElseThrow();

        assertEquals("NET-001", alert.ruleId());
        assertEquals(3, alert.observedCount());

        assertEquals(
                3,
                bounded.trackedObservationCount(
                        "192.0.2.10",
                        "192.0.2.20"
                )
        );
    }

}
