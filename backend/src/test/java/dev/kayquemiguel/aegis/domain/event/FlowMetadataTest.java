
package dev.kayquemiguel.aegis.domain.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlowMetadataTest {

    @Test
    void shouldCreateMetadataWithStateAndReason() {
        FlowMetadata metadata = new FlowMetadata(
                "established",
                "timeout"
        );

        assertEquals("established", metadata.state());
        assertEquals("timeout", metadata.reason());
    }

    @Test
    void shouldAllowOnlyState() {
        FlowMetadata metadata =
                new FlowMetadata("new", null);

        assertEquals("new", metadata.state());
        assertNull(metadata.reason());
    }

    @Test
    void shouldAllowOnlyReason() {
        FlowMetadata metadata =
                new FlowMetadata(null, "shutdown");

        assertNull(metadata.state());
        assertEquals("shutdown", metadata.reason());
    }

    @Test
    void shouldTrimValues() {
        FlowMetadata metadata = new FlowMetadata(
                "  closed  ",
                "  timeout  "
        );

        assertEquals("closed", metadata.state());
        assertEquals("timeout", metadata.reason());
    }

    @Test
    void shouldRejectEmptyMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FlowMetadata(null, null)
        );
    }

    @Test
    void shouldRejectBlankState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FlowMetadata("  ", null)
        );
    }

    @Test
    void shouldRejectExcessivelyLongReason() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FlowMetadata(
                        "closed",
                        "x".repeat(129)
                )
        );
    }
}
