
package dev.kayquemiguel.aegis.detection.api;

import dev.kayquemiguel.aegis.detection.DetectionAlert;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertRepository;
import dev.kayquemiguel.aegis.detection.persistence.DetectionAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class DetectionAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DetectionAlertService service;

    @Autowired
    private DetectionAlertRepository repository;

    private static final Instant BASE =
            Instant.parse("2026-11-03T10:00:00Z");

    private static String sensor() {
        return "test-" + UUID.randomUUID();
    }

    private UUID save(
            String sensorId,
            String ruleId,
            long seconds
    ) {
        DetectionAlert alert = new DetectionAlert(
                ruleId,
                "192.0.2.10",
                "192.0.2.20",
                BASE.plusSeconds(seconds),
                3,
                3,
                "Synthetic alert for HTTP integration test",
                sensorId
        );

        UUID id = service.save(alert);
        repository.flush();
        return id;
    }

    @Test
    void shouldListAlertsWithPagination() throws Exception {
        String sensorId = sensor();

        save(sensorId, "NET-001", 0);
        save(sensorId, "NET-001", 10);
        save(sensorId, "NET-001", 20);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("sensorId", sensorId)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].sensorId")
                        .value(sensorId))
                .andExpect(jsonPath("$.content[0].detectedAt")
                        .value("2026-11-03T10:00:20Z"));
    }

    @Test
    void shouldReadSecondPage() throws Exception {
        String sensorId = sensor();

        save(sensorId, "NET-001", 0);
        save(sensorId, "NET-001", 10);
        save(sensorId, "NET-001", 20);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("sensorId", sensorId)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].detectedAt")
                        .value("2026-11-03T10:00:00Z"));
    }

    @Test
    void shouldFilterBySensorAndRule() throws Exception {
        String sensorA = sensor();
        String sensorB = sensor();

        save(sensorA, "NET-001", 0);
        save(sensorA, "NET-002", 10);
        save(sensorB, "NET-001", 20);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("sensorId", sensorA)
                        .param("ruleId", "NET-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sensorId")
                        .value(sensorA))
                .andExpect(jsonPath("$.content[0].ruleId")
                        .value("NET-001"));
    }

    @Test
    void shouldFilterByRuleOnly() throws Exception {
        String sensorId = sensor();
        String rule = "TEST-" + UUID.randomUUID();

        save(sensorId, rule, 0);
        save(sensorId, "NET-001", 10);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("ruleId", rule))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ruleId")
                        .value(rule));
    }

    @Test
    void shouldFindAlertById() throws Exception {
        String sensorId = sensor();
        UUID id = save(sensorId, "NET-001", 0);

        mockMvc.perform(get("/api/v1/alerts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(id.toString()))
                .andExpect(jsonPath("$.ruleId")
                        .value("NET-001"))
                .andExpect(jsonPath("$.sensorId")
                        .value(sensorId))
                .andExpect(jsonPath("$.distinctPorts")
                        .value(3))
                .andExpect(jsonPath("$.threshold")
                        .value(3));
    }

    @Test
    void shouldReturn404ForMissingAlert() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/alerts/{id}",
                        UUID.randomUUID()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/alerts")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/alerts")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidFiltersAndIds() throws Exception {
        mockMvc.perform(get("/api/v1/alerts")
                        .param("sensorId", " "))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/alerts")
                        .param("ruleId", "NET/001"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/alerts/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
