package dev.kayquemiguel.aegis.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetworkEventTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-10-31T10:00:00Z");

    private static final Instant INGESTED_AT =
            Instant.parse("2026-10-31T10:00:05Z");

    private NetworkEvent createEvent(
            String sourceIp,
            String destinationIp,
            Integer sourcePort,
            Integer destinationPort,
            NetworkProtocol protocol,
            String sourceSystem
    ) {
        return new NetworkEvent(
                UUID.randomUUID(),
                OCCURRED_AT,
                INGESTED_AT,
                sourceIp,
                destinationIp,
                sourcePort,
                destinationPort,
                protocol,
                sourceSystem
        );
    }

    @Test
    void shouldCreateValidTcpEvent() {
        NetworkEvent event = createEvent(
                "192.0.2.10",
                "192.0.2.20",
                49152,
                443,
                NetworkProtocol.TCP,
                "suricata"
        );

        assertEquals("192.0.2.10", event.sourceIp());
        assertEquals("192.0.2.20", event.destinationIp());
        assertEquals(443, event.destinationPort());
        assertEquals(NetworkProtocol.TCP, event.protocol());
        assertEquals("suricata", event.sourceSystem());
        assertNotNull(event.id());
    }

    @Test
    void shouldAcceptValidIpv6() {
        NetworkEvent event = createEvent(
                "2001:db8::1",
                "2001:db8::2",
                12345,
                443,
                NetworkProtocol.TCP,
                "zeek"
        );

        assertNotNull(event.sourceIp());
        assertNotNull(event.destinationIp());
        assertEquals(NetworkProtocol.TCP, event.protocol());
    }

    @Test
    void shouldAcceptMissingPorts() {
        NetworkEvent event = createEvent(
                "192.0.2.10",
                "192.0.2.20",
                null,
                null,
                NetworkProtocol.OTHER,
                "suricata"
        );

        assertNull(event.sourcePort());
        assertNull(event.destinationPort());
    }

    @Test
    void shouldAcceptIcmpWithoutPorts() {
        NetworkEvent event = createEvent(
                "192.0.2.10",
                "192.0.2.20",
                null,
                null,
                NetworkProtocol.ICMP,
                "suricata"
        );

        assertEquals(NetworkProtocol.ICMP, event.protocol());
    }

    @Test
    void shouldRejectIcmpWithPorts() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "192.0.2.10",
                        "192.0.2.20",
                        1234,
                        80,
                        NetworkProtocol.ICMP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldRejectInvalidIpv4() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "999.10.10.10",
                        "192.0.2.20",
                        1234,
                        80,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldRejectHostnameInsteadOfIp() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "example.com",
                        "192.0.2.20",
                        1234,
                        80,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldRejectLeadingZeroesInIpv4() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "192.000.2.10",
                        "192.0.2.20",
                        1234,
                        80,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldRejectInvalidPort() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "192.0.2.10",
                        "192.0.2.20",
                        5000,
                        70000,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldRejectBlankSourceSystem() {
        assertThrows(IllegalArgumentException.class, () ->
                createEvent(
                        "192.0.2.10",
                        "192.0.2.20",
                        5000,
                        443,
                        NetworkProtocol.TCP,
                        " "
                )
        );
    }

    @Test
    void shouldRejectMissingTimestamp() {
        assertThrows(NullPointerException.class, () ->
                new NetworkEvent(
                        UUID.randomUUID(),
                        null,
                        INGESTED_AT,
                        "192.0.2.10",
                        "192.0.2.20",
                        5000,
                        443,
                        NetworkProtocol.TCP,
                        "suricata"
                )
        );
    }

    @Test
    void shouldTrimSourceSystem() {
        NetworkEvent event = createEvent(
                "192.0.2.10",
                "192.0.2.20",
                5000,
                443,
                NetworkProtocol.TCP,
                "  suricata  "
        );

        assertEquals("suricata", event.sourceSystem());
    }
}
