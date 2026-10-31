package dev.kayquemiguel.aegis.ingestion.suricata;

import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import dev.kayquemiguel.aegis.domain.event.NetworkProtocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SuricataFlowParserTest {

    private final SuricataFlowParser parser =
            new SuricataFlowParser();

    private final Instant ingestedAt =
            Instant.parse("2026-10-31T10:01:00Z");

    @Test
    void shouldParseValidFlow() {
        String json = """
                {
                  "timestamp": "2026-10-31T10:00:00.123456+0000",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "src_port": 49152,
                  "dest_ip": "192.0.2.20",
                  "dest_port": 443,
                  "proto": "TCP",
                  "flow_id": 12345
                }
                """;

        NetworkEvent event = parser.parse(json, ingestedAt)
                .orElseThrow();

        assertEquals("192.0.2.10", event.sourceIp());
        assertEquals("192.0.2.20", event.destinationIp());
        assertEquals(443, event.destinationPort());
        assertEquals(NetworkProtocol.TCP, event.protocol());
        assertEquals(
                Instant.parse("2026-10-31T10:00:00.123456Z"),
                event.occurredAt()
        );
    }

    @Test
    void shouldIgnoreNonFlowEvent() {
        String json = """
                {
                  "event_type": "alert",
                  "timestamp": "2026-10-31T10:00:00Z"
                }
                """;

        assertTrue(parser.parse(json, ingestedAt).isEmpty());
    }

    @Test
    void shouldRejectInvalidJson() {
        assertThrows(IllegalArgumentException.class, () ->
                parser.parse("{invalid}", ingestedAt)
        );
    }

    @Test
    void shouldRejectMissingSourceIp() {
        String json = """
                {
                  "timestamp": "2026-10-31T10:00:00Z",
                  "event_type": "flow",
                  "dest_ip": "192.0.2.20",
                  "proto": "TCP"
                }
                """;

        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(json, ingestedAt)
        );
    }

    @Test
    void shouldRejectInvalidPort() {
        String json = """
                {
                  "timestamp": "2026-10-31T10:00:00Z",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "dest_ip": "192.0.2.20",
                  "dest_port": 70000,
                  "proto": "TCP"
                }
                """;

        assertThrows(IllegalArgumentException.class, () ->
                parser.parse(json, ingestedAt)
        );
    }
}
