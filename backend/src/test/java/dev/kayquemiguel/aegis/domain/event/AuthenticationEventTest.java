package dev.kayquemiguel.aegis.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationEventTest {

    private AuthenticationEvent event(
            String sourceIp,
            String username,
            String sensorId,
            AuthenticationOutcome outcome
    ) {
        return new AuthenticationEvent(
                UUID.randomUUID(),
                Instant.parse("2026-11-16T10:00:00Z"),
                Instant.parse("2026-11-16T10:00:01Z"),
                sourceIp,
                username,
                "auth-log",
                sensorId,
                outcome
        );
    }

    @Test
    void shouldCreateFailedAuthenticationEvent() {
        AuthenticationEvent event = event(
                "192.0.2.10",
                "admin",
                "sensor-001",
                AuthenticationOutcome.FAILURE
        );

        assertTrue(event.isFailure());
        assertEquals("admin", event.username());
        assertEquals("sensor-001", event.sensorId());
    }

    @Test
    void shouldCreateSuccessfulAuthenticationEvent() {
        AuthenticationEvent event = event(
                "192.0.2.10",
                "admin",
                "sensor-001",
                AuthenticationOutcome.SUCCESS
        );

        assertFalse(event.isFailure());
    }

    @Test
    void shouldTrimUsername() {
        AuthenticationEvent event = event(
                "192.0.2.10",
                "  admin  ",
                "sensor-001",
                AuthenticationOutcome.FAILURE
        );

        assertEquals("admin", event.username());
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(
                IllegalArgumentException.class,
                () -> event(
                        "192.0.2.10",
                        " ",
                        "sensor-001",
                        AuthenticationOutcome.FAILURE
                )
        );
    }

    @Test
    void shouldRejectInvalidSensor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> event(
                        "192.0.2.10",
                        "admin",
                        "invalid sensor!",
                        AuthenticationOutcome.FAILURE
                )
        );
    }

    @Test
    void shouldRejectInvalidSourceIp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> event(
                        "999.999.999.999",
                        "admin",
                        "sensor-001",
                        AuthenticationOutcome.FAILURE
                )
        );
    }

    @Test
    void shouldValidateStrictIpAddressFormats() {
        assertEquals(
                "192.0.2.10",
                event(
                        "192.0.2.10",
                        "admin",
                        "sensor-001",
                        AuthenticationOutcome.FAILURE
                ).sourceIp()
        );

        assertNotNull(event(
                "2001:db8::1",
                "admin",
                "sensor-001",
                AuthenticationOutcome.FAILURE
        ));

        for (String invalid : new String[]{
                "127.1",
                "192.168.001.2",
                "999.999.999.999",
                "example.com",
                ":::",
                "192.0.2.10:443",
                "192.0.2.10/24"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> event(
                            invalid,
                            "admin",
                            "sensor-001",
                            AuthenticationOutcome.FAILURE
                    ),
                    "Expected rejection: " + invalid
            );
        }
    }

}
