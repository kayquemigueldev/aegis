
package dev.kayquemiguel.aegis.security;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SensorAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DetectionAlertService service;

    @Autowired
    private DetectionAlertRepository repository;

    private static String sensor() {
        return "test-" + UUID.randomUUID();
    }

    private UUID save(String sensorId) {
        DetectionAlert alert = new DetectionAlert(
                "NET-001",
                "192.0.2.10",
                "192.0.2.20",
                Instant.parse("2026-11-03T10:00:00Z"),
                3,
                3,
                "Synthetic sensor authorization test",
                sensorId
        );

        UUID id = service.save(alert);
        repository.flush();

        return id;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor
    analyst(String... sensors) {

        var authorities =
                new java.util.ArrayList<
                        SimpleGrantedAuthority>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_ANALYST"
                )
        );

        for (String sensorId : sensors) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "SENSOR:" + sensorId
                    )
            );
        }

        return user("analyst")
                .authorities(
                        authorities.toArray(
                                SimpleGrantedAuthority[]::new
                        )
                );
    }

    @Test
    void shouldListOnlyAuthorizedSensors()
            throws Exception {

        String allowed = sensor();
        String denied = sensor();

        save(allowed);
        save(denied);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(analyst(allowed))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].sensorId")
                                .value(allowed)
                );
    }

    @Test
    void shouldAllowExplicitAuthorizedSensor()
            throws Exception {

        String allowed = sensor();

        save(allowed);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param(
                                        "sensorId",
                                        allowed
                                )
                                .with(analyst(allowed))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }

    @Test
    void shouldRejectExplicitUnauthorizedSensor()
            throws Exception {

        String allowed = sensor();
        String denied = sensor();

        save(denied);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param(
                                        "sensorId",
                                        denied
                                )
                                .with(analyst(allowed))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHideUnauthorizedAlertById()
            throws Exception {

        String allowed = sensor();
        String denied = sensor();

        UUID hiddenId = save(denied);

        mockMvc.perform(
                        get(
                                "/api/v1/alerts/{id}",
                                hiddenId
                        )
                                .with(analyst(allowed))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowAuthorizedAlertById()
            throws Exception {

        String allowed = sensor();
        UUID id = save(allowed);

        mockMvc.perform(
                        get(
                                "/api/v1/alerts/{id}",
                                id
                        )
                                .with(analyst(allowed))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sensorId")
                                .value(allowed)
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(id.toString())
                );
    }

    @Test
    void shouldAllowAdministratorToSeeAllSensors()
            throws Exception {

        String sensorA = sensor();
        String sensorB = sensor();

        save(sensorA);
        save(sensorB);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(
                                        org.hamcrest.Matchers
                                                .greaterThanOrEqualTo(2)
                                )
                );
    }

    @Test
    void shouldReturnNoAlertsWithoutSensorPermission()
            throws Exception {

        String existingSensor = sensor();

        save(existingSensor);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(analyst())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(0)
                );
    }
}
