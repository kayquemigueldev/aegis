
package dev.kayquemiguel.aegis.security;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aegis.security.mode=database",
        "aegis.bootstrap.admin.username=",
        "aegis.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class DatabaseAuthenticationIntegrationTest {

    private static final String PASSWORD =
            "DatabaseTestPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DetectionAlertService alertService;

    @Autowired
    private DetectionAlertRepository alertRepository;

    private String createUser(
            String role,
            boolean enabled,
            String... sensors
    ) {
        UUID id = UUID.randomUUID();
        String username = "db-" + id;

        jdbc.update(
                """
                INSERT INTO aegis_users (
                    id, username, password_hash,
                    role, enabled
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                username,
                passwordEncoder.encode(PASSWORD),
                role,
                enabled
        );

        for (String sensorId : sensors) {
            jdbc.update(
                    """
                    INSERT INTO aegis_user_sensors (
                        user_id, sensor_id
                    )
                    VALUES (?, ?)
                    """,
                    id,
                    sensorId
            );
        }

        return username;
    }

    private UUID saveAlert(String sensorId) {
        DetectionAlert alert = new DetectionAlert(
                "NET-001",
                "192.0.2.10",
                "192.0.2.20",
                Instant.parse("2026-11-03T10:00:00Z"),
                3,
                3,
                "Database authentication test",
                sensorId
        );

        UUID id = alertService.save(alert);
        alertRepository.flush();

        return id;
    }

    private static String sensor() {
        return "sensor-" + UUID.randomUUID();
    }

    @Test
    void shouldAuthenticateDatabaseAnalyst()
            throws Exception {

        String allowed = sensor();
        String username = createUser(
                "ANALYST", true, allowed
        );

        saveAlert(allowed);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param("sensorId", allowed)
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements").value(1)
                );
    }

    @Test
    void shouldRejectIncorrectDatabasePassword()
            throws Exception {

        String username = createUser(
                "ANALYST", true, sensor()
        );

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(httpBasic(
                                        username,
                                        "wrong-password"
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDisabledDatabaseUser()
            throws Exception {

        String username = createUser(
                "ANALYST", false, sensor()
        );

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownDatabaseUser()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(httpBasic(
                                        "unknown-" + UUID.randomUUID(),
                                        PASSWORD
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyOtherSensorToDatabaseAnalyst()
            throws Exception {

        String allowed = sensor();
        String denied = sensor();

        String username = createUser(
                "ANALYST", true, allowed
        );

        saveAlert(denied);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param("sensorId", denied)
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHideOtherSensorAlertById()
            throws Exception {

        String allowed = sensor();
        String denied = sensor();

        String username = createUser(
                "ANALYST", true, allowed
        );

        UUID hiddenId = saveAlert(denied);

        mockMvc.perform(
                        get(
                                "/api/v1/alerts/{id}",
                                hiddenId
                        )
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowDatabaseAdminToReadAllSensors()
            throws Exception {

        String username = createUser(
                "ADMIN", true
        );

        String targetSensor = sensor();

        saveAlert(targetSensor);

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param(
                                        "sensorId",
                                        targetSensor
                                )
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements").value(1)
                );
    }

    @Test
    void shouldReturnNoAlertsWithoutAssignedSensors()
            throws Exception {

        String username = createUser(
                "ANALYST", true
        );

        saveAlert(sensor());

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(httpBasic(
                                        username, PASSWORD
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements").value(0)
                )
                .andExpect(
                        jsonPath("$.content.length()").value(0)
                );
    }
}

