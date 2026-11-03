
package dev.kayquemiguel.aegis.detection;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import dev.kayquemiguel.aegis.ingestion.suricata.SuricataFlowParser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SensorIsolationTest {

    private static final Instant BASE =
            Instant.parse("2026-11-03T10:00:00Z");

    private static final String SOURCE = "192.0.2.10";
    private static final String DESTINATION = "192.0.2.20";

    private static PossiblePortScanDetector detector() {
        return new PossiblePortScanDetector(
                Duration.ofSeconds(60),
                3
        );
    }

    private static NetworkEvent event(
            String sensorId,
            int destinationPort,
            String flowId,
            long seconds
    ) {
        Instant timestamp = BASE.plusSeconds(seconds);

        return new NetworkEvent(
                UUID.randomUUID(),
                timestamp,
                timestamp.plusSeconds(1),
                SOURCE,
                DESTINATION,
                49152,
                destinationPort,
                NetworkProtocol.TCP,
                "suricata",
                flowId,
                null,
                sensorId
        );
    }

    @Test
    void shouldNotMixPortsFromDifferentSensors() {
        PossiblePortScanDetector detector = detector();

        assertTrue(detector.evaluate(
                event("sensor-a", 22, "1", 0)
        ).isEmpty());

        assertTrue(detector.evaluate(
                event("sensor-b", 80, "2", 10)
        ).isEmpty());

        assertTrue(detector.evaluate(
                event("sensor-a", 443, "3", 20)
        ).isEmpty());

        assertEquals(2, detector.trackedPairCount());
    }

    @Test
    void shouldGenerateIndependentAlerts() {
        PossiblePortScanDetector detector = detector();

        detector.evaluate(event("sensor-a", 22, "1", 0));
        detector.evaluate(event("sensor-a", 80, "2", 10));

        detector.evaluate(event("sensor-b", 22, "1", 0));
        detector.evaluate(event("sensor-b", 80, "2", 10));

        DetectionAlert first = detector.evaluate(
                event("sensor-a", 443, "3", 20)
        ).orElseThrow();

        DetectionAlert second = detector.evaluate(
                event("sensor-b", 443, "3", 20)
        ).orElseThrow();

        assertEquals("sensor-a", first.sensorId());
        assertEquals("sensor-b", second.sensorId());

        assertEquals(3, first.distinctPorts());
        assertEquals(3, second.distinctPorts());
        assertEquals(2, detector.trackedPairCount());
    }

    @Test
    void shouldDeduplicateFlowOnlyWithinItsSensor() {
        PossiblePortScanDetector detector = detector();

        detector.evaluate(event("sensor-a", 22, "100", 0));
        detector.evaluate(event("sensor-b", 80, "100", 1));

        assertEquals(
                1,
                detector.trackedObservationCount(
                        "sensor-a", SOURCE, DESTINATION
                )
        );

        assertEquals(
                1,
                detector.trackedObservationCount(
                        "sensor-b", SOURCE, DESTINATION
                )
        );

        assertTrue(detector.evaluate(
                event("sensor-a", 443, "100", 2)
        ).isEmpty());

        assertEquals(
                1,
                detector.trackedObservationCount(
                        "sensor-a", SOURCE, DESTINATION
                )
        );
    }

    @Test
    void shouldPreserveLegacyCompatibility() {
        NetworkEvent event = new NetworkEvent(
                UUID.randomUUID(),
                BASE,
                BASE.plusSeconds(1),
                SOURCE,
                DESTINATION,
                49152,
                443,
                NetworkProtocol.TCP,
                "suricata",
                "999"
        );

        assertEquals(
                NetworkEvent.LEGACY_SENSOR_ID,
                event.sensorId()
        );
    }

    @Test
    void shouldRejectInvalidSensorIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> event(" ", 22, "1", 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event("sensor/a", 22, "1", 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event("x".repeat(65), 22, "1", 0)
        );
    }

    @Test
    void shouldAttachConfiguredSensorToParsedEvent() {
        SuricataFlowParser parser =
                new SuricataFlowParser("sensor-brusque-01");

        String json = """
                {
                  "timestamp": "2026-11-03T10:00:00Z",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "dest_ip": "192.0.2.20",
                  "src_port": 49152,
                  "dest_port": 443,
                  "proto": "TCP",
                  "flow_id": 123
                }
                """;

        NetworkEvent parsed = parser.parse(
                json,
                BASE.plusSeconds(1)
        ).orElseThrow();

        assertEquals(
                "sensor-brusque-01",
                parsed.sensorId()
        );

        assertEquals("suricata", parsed.sourceSystem());
        assertEquals("123", parsed.sourceFlowId());
    }

    @Test
    void shouldRejectInvalidParserSensor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SuricataFlowParser("sensor inválido")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SuricataFlowParser(null)
        );
    }
}
