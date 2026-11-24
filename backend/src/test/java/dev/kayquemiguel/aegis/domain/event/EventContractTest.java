package dev.kayquemiguel.aegis.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventContractTest {

    private static final Instant OCCURRED =
            Instant.parse("2026-11-24T10:00:00Z");

    private static final Instant INGESTED =
            Instant.parse("2026-11-24T10:00:01Z");

    private static NetworkEvent network(
            String sensor
    ) {
        return new NetworkEvent(
                UUID.randomUUID(),
                OCCURRED,
                INGESTED,
                "192.0.2.10",
                "198.51.100.20",
                51000,
                443,
                NetworkProtocol.TCP,
                "suricata",
                "flow-contract-001",
                new FlowMetadata(
                        "established", "normal"
                ),
                sensor
        );
    }

    @Test
    void shouldPreserveNetworkEventIdentity() {
        NetworkEvent event = network("sensor-01");

        assertNotNull(event.id());
        assertEquals("sensor-01", event.sensorId());
        assertEquals("suricata", event.sourceSystem());
        assertEquals("flow-contract-001",
                event.sourceFlowId());
        assertEquals(OCCURRED, event.occurredAt());
        assertEquals(INGESTED, event.ingestedAt());
        assertEquals(443, event.destinationPort());
    }

    @Test
    void shouldRejectInvalidSensorIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> network("sensor invalid")
        );
    }

    @Test
    void shouldRejectInvalidNetworkAddress() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NetworkEvent(
                        UUID.randomUUID(),
                        OCCURRED,
                        INGESTED,
                        "999.999.999.999",
                        "198.51.100.20",
                        50000,
                        443,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldPreserveAuthenticationOutcome() {
        AuthenticationEvent event =
                new AuthenticationEvent(
                        UUID.randomUUID(),
                        OCCURRED,
                        INGESTED,
                        "192.0.2.10",
                        "analyst",
                        "auth-lab",
                        "sensor-01",
                        AuthenticationOutcome.FAILURE
                );

        assertTrue(event.isFailure());
        assertEquals("analyst", event.username());
        assertEquals("sensor-01", event.sensorId());
    }

    @Test
    void shouldRejectInvalidAuthenticationSensor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticationEvent(
                        UUID.randomUUID(),
                        OCCURRED,
                        INGESTED,
                        "192.0.2.10",
                        "analyst",
                        "auth-lab",
                        "invalid sensor",
                        AuthenticationOutcome.FAILURE
                )
        );
    }
}
