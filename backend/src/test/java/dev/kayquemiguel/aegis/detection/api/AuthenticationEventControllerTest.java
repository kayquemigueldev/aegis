package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DetectionAlertRepository repository;

    private String payload(UUID id) {
        return """
                {
                  "id": "%s",
                  "occurredAt": "%s",
                  "sourceIp": "192.0.2.10",
                  "username": "admin",
                  "sourceSystem": "auth-log",
                  "outcome": "FAILURE"
                }
                """.formatted(id, Instant.now());
    }

    @Test
    @WithMockUser(authorities = {
            "ROLE_ANALYST",
            "SENSOR:sensor-allowed"
    })
    void shouldAcceptAuthorizedSensorEvent() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/sensors/sensor-allowed/auth-events"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.alertGenerated").value(false));
    }

    @Test
    @WithMockUser(authorities = {
            "ROLE_ANALYST",
            "SENSOR:sensor-allowed"
    })
    void shouldRejectUnauthorizedSensorEvent() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/sensors/sensor-denied/auth-events"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/sensors/sensor-allowed/auth-events"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPersistAlertAfterFiveHttpEvents()
            throws Exception {
        String sensorId = "test-" + UUID.randomUUID();

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post(
                            "/api/v1/sensors/{sensorId}/auth-events",
                            sensorId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(UUID.randomUUID())))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post(
                        "/api/v1/sensors/{sensorId}/auth-events",
                        sensorId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertGenerated").value(true))
                .andExpect(jsonPath("$.alertId").isNotEmpty());

        repository.flush();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotGenerateAlertFromRepeatedEventId()
            throws Exception {
        String sensorId = "test-" + UUID.randomUUID();
        UUID duplicateId = UUID.randomUUID();

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(
                            "/api/v1/sensors/{sensorId}/auth-events",
                            sensorId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(duplicateId)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.alertGenerated")
                            .value(false));
        }

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(
                            "/api/v1/sensors/{sensorId}/auth-events",
                            sensorId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(UUID.randomUUID())))
                    .andExpect(status().isAccepted());
        }

        // 1 evento original + 3 novos = 4 falhas.
        // O quinto evento unico deve gerar o alerta.
        mockMvc.perform(post(
                        "/api/v1/sensors/{sensorId}/auth-events",
                        sensorId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertGenerated")
                        .value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidAuthenticationEvent()
            throws Exception {
        String sensorId = "test-" + UUID.randomUUID();

        String invalidPayload = """
                {
                  "id": "%s",
                  "occurredAt": "%s",
                  "sourceIp": "invalid-ip",
                  "username": "admin",
                  "sourceSystem": "auth-log",
                  "outcome": "FAILURE"
                }
                """.formatted(
                        UUID.randomUUID(),
                        Instant.now()
                );

        mockMvc.perform(post(
                        "/api/v1/sensors/{sensorId}/auth-events",
                        sensorId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
                .andExpect(status().isBadRequest());

        String missingOutcome = """
                {
                  "id": "%s",
                  "occurredAt": "%s",
                  "sourceIp": "192.0.2.10",
                  "username": "admin",
                  "sourceSystem": "auth-log"
                }
                """.formatted(
                        UUID.randomUUID(),
                        Instant.now()
                );

        mockMvc.perform(post(
                        "/api/v1/sensors/{sensorId}/auth-events",
                        sensorId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingOutcome))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectEventsOutsideAllowedTimeRange()
            throws Exception {
        String sensorId = "test-" + UUID.randomUUID();

        for (Instant occurredAt : new Instant[] {
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(172800)
        }) {
            String json = """
                    {
                      "id": "%s",
                      "occurredAt": "%s",
                      "sourceIp": "192.0.2.10",
                      "username": "admin",
                      "sourceSystem": "auth-log",
                      "outcome": "FAILURE"
                    }
                    """.formatted(
                            UUID.randomUUID(),
                            occurredAt
                    );

            mockMvc.perform(post(
                            "/api/v1/sensors/{sensorId}/auth-events",
                            sensorId
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

}
