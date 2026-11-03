
package dev.kayquemiguel.aegis.ingestion.suricata;

import dev.kayquemiguel.aegis.domain.event.FlowMetadata;
import dev.kayquemiguel.aegis.domain.event.NetworkEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SuricataFlowMetadataTest {

    private final SuricataFlowParser parser =
            new SuricataFlowParser();

    private final Instant ingestedAt =
            Instant.parse("2026-10-31T10:01:00Z");

    private String flowEvent(String flowObject) {
        return """
                {
                  "timestamp": "2026-10-31T10:00:00Z",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "dest_ip": "192.0.2.20",
                  "src_port": 49152,
                  "dest_port": 443,
                  "proto": "TCP",
                  "flow_id": 12345,
                  "flow": %s
                }
                """.formatted(flowObject);
    }

    @Test
    void shouldPreserveStateAndReason() {
        String json = flowEvent("""
                {
                  "state": "established",
                  "reason": "timeout"
                }
                """);

        NetworkEvent event = parser.parse(
                json, ingestedAt
        ).orElseThrow();

        FlowMetadata metadata = event.flowMetadata();

        assertNotNull(metadata);
        assertEquals("established", metadata.state());
        assertEquals("timeout", metadata.reason());
        assertEquals("12345", event.sourceFlowId());
    }

    @Test
    void shouldPreserveClosedState() {
        NetworkEvent event = parser.parse(
                flowEvent("""
                        {"state": "closed"}
                        """),
                ingestedAt
        ).orElseThrow();

        assertEquals(
                "closed",
                event.flowMetadata().state()
        );
    }

    @Test
    void shouldAcceptMissingFlowObject() {
        String json = """
                {
                  "timestamp": "2026-10-31T10:00:00Z",
                  "event_type": "flow",
                  "src_ip": "192.0.2.10",
                  "dest_ip": "192.0.2.20",
                  "proto": "TCP"
                }
                """;

        NetworkEvent event = parser.parse(
                json, ingestedAt
        ).orElseThrow();

        assertNull(event.flowMetadata());
    }

    @Test
    void shouldAcceptEmptyFlowObject() {
        NetworkEvent event = parser.parse(
                flowEvent("{}"),
                ingestedAt
        ).orElseThrow();

        assertNull(event.flowMetadata());
    }

    @Test
    void shouldRejectInvalidFlowObject() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        flowEvent("\"invalid\""),
                        ingestedAt
                )
        );
    }

    @Test
    void shouldRejectNumericFlowState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        flowEvent("{\"state\": 123}"),
                        ingestedAt
                )
        );
    }

    @Test
    void shouldRejectBlankFlowReason() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        flowEvent("{\"reason\": \"  \"}"),
                        ingestedAt
                )
        );
    }
}
