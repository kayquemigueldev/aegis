package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.application.DetectionAlertQueryService;

import dev.kayquemiguel.aegis.detection.application.DetectionAlertReadPort;
import dev.kayquemiguel.aegis.security.SensorAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DetectionAlertQueryServiceSecurityTest {

    @Test
    void sensorOnlyPrincipalCannotQueryAlertsInternally() {
        DetectionAlertReadPort readPort =
                mock(DetectionAlertReadPort.class);

        DetectionAlertQueryService service =
                new DetectionAlertQueryService(
                        readPort,
                        new SensorAccessPolicy()
                );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "sensor-only",
                        "unused",
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SENSOR"
                                ),
                                new SimpleGrantedAuthority(
                                        "SENSOR:sensor-allowed"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.search(
                                null,
                                null,
                                0,
                                20,
                                authentication
                        )
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(readPort);
    }
}
