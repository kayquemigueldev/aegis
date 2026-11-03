
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PossiblePortScanMemoryTest {

    private static final Instant BASE =
            Instant.parse("2026-11-03T10:00:00Z");

    private static final String DESTINATION = "192.0.2.20";

    private static PossiblePortScanDetector detector(
            int maxPairs,
            int maxObservations
    ) {
        return new PossiblePortScanDetector(
                Duration.ofSeconds(60),
                3,
                Duration.ofSeconds(30),
                maxPairs,
                maxObservations
        );
    }

    private static NetworkEvent event(
            String source,
            int port,
            String flowId,
            long second
    ) {
        Instant timestamp = BASE.plusSeconds(second);

        return new NetworkEvent(
                UUID.randomUUID(),
                timestamp,
                timestamp.plusSeconds(1),
                source,
                DESTINATION,
                49152,
                port,
                NetworkProtocol.TCP,
                "suricata",
                flowId
        );
    }

    @Test
    void shouldLimitTrackedPairs() {
        PossiblePortScanDetector detector = detector(2, 5);

        detector.evaluate(event("192.0.2.1", 22, "1", 0));
        detector.evaluate(event("192.0.2.2", 22, "2", 1));
        detector.evaluate(event("192.0.2.3", 22, "3", 2));

        assertEquals(2, detector.trackedPairCount());

        assertEquals(
                0,
                detector.trackedObservationCount(
                        "192.0.2.1",
                        DESTINATION
                )
        );
    }

    @Test
    void shouldExpireInactivePairs() {
        PossiblePortScanDetector detector = detector(10, 5);

        detector.evaluate(event("192.0.2.1", 22, "1", 0));

        assertEquals(1, detector.trackedPairCount());

        detector.evaluate(event("192.0.2.2", 80, "2", 40));

        assertEquals(1, detector.trackedPairCount());

        assertEquals(
                0,
                detector.trackedObservationCount(
                        "192.0.2.1",
                        DESTINATION
                )
        );
    }

    @Test
    void shouldLimitObservationsPerPair() {
        PossiblePortScanDetector detector = detector(10, 3);

        String source = "192.0.2.10";

        detector.evaluate(event(source, 22, "1", 0));
        detector.evaluate(event(source, 80, "2", 1));

        assertTrue(
                detector.evaluate(
                        event(source, 443, "3", 2)
                ).isPresent()
        );

        detector.evaluate(event(source, 8080, "4", 3));
        detector.evaluate(event(source, 8443, "5", 4));

        assertEquals(
                3,
                detector.trackedObservationCount(
                        source,
                        DESTINATION
                )
        );
    }

    @Test
    void shouldDiscardExcessivelyLateEvents() {
        PossiblePortScanDetector detector = detector(10, 5);

        detector.evaluate(event("192.0.2.1", 22, "1", 100));

        assertTrue(
                detector.evaluate(
                        event("192.0.2.2", 80, "2", 0)
                ).isEmpty()
        );

        assertEquals(1, detector.trackedPairCount());
    }

    @Test
    void shouldRestartDetectionAfterIdleExpiration() {
        PossiblePortScanDetector detector = detector(10, 5);
        String source = "192.0.2.10";

        detector.evaluate(event(source, 22, "1", 0));
        detector.evaluate(event(source, 80, "2", 1));

        // A janela de inatividade e superada.
        assertTrue(
                detector.evaluate(
                        event(source, 443, "3", 40)
                ).isEmpty()
        );

        assertTrue(
                detector.evaluate(
                        event(source, 8080, "4", 41)
                ).isEmpty()
        );

        assertTrue(
                detector.evaluate(
                        event(source, 8443, "5", 42)
                ).isPresent()
        );
    }

    @Test
    void shouldRejectInvalidMemoryLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> detector(0, 5)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> detector(5, 2)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PossiblePortScanDetector(
                        Duration.ofSeconds(60),
                        3,
                        Duration.ZERO,
                        10,
                        5
                )
        );
    }
}
