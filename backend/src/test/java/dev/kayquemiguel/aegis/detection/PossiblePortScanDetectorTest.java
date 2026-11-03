
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
        detector.evaluate(tcp(80, "2", 20));

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
}
