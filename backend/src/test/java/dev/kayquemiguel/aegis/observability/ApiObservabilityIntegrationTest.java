package dev.kayquemiguel.aegis.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aegis.security.mode=local",
        "aegis.security.username=",
        "aegis.security.password="
})
@AutoConfigureMockMvc
class ApiObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateCorrelationId()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(user("analyst")
                                        .roles("ANALYST"))
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER,
                                org.hamcrest.Matchers.matchesPattern(
                                        "[0-9a-f-]{36}"
                                )
                        )
                );
    }

    @Test
    void shouldPreserveValidCorrelationId()
            throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .header(
                                        CorrelationIdFilter.HEADER,
                                        id
                                )
                                .with(user("analyst")
                                        .roles("ANALYST"))
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER,
                                id
                        )
                );
    }

    @Test
    void shouldReplaceUntrustedCorrelationId()
            throws Exception {
        String malicious =
                "invalid-id-with-control-data";

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .header(
                                        CorrelationIdFilter.HEADER,
                                        malicious
                                )
                                .with(user("analyst")
                                        .roles("ANALYST"))
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER,
                                org.hamcrest.Matchers.not(
                                        malicious
                                )
                        )
                );
    }

    @Test
    void shouldReturnSanitizedValidationError()
            throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .param("page", "-1")
                                .header(
                                        CorrelationIdFilter.HEADER,
                                        id
                                )
                                .with(user("analyst")
                                        .roles("ANALYST"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code").value(
                                "BAD_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "Invalid request"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(id)
                );
    }

    @Test
    void shouldPreserveAuthenticationStatus()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/alerts")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        header().exists(
                                CorrelationIdFilter.HEADER
                        )
                );
    }

    @Test
    void shouldReturnSanitizedNotFound()
            throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(
                        get("/api/v1/alerts/{id}",
                                UUID.randomUUID())
                                .header(
                                        CorrelationIdFilter.HEADER,
                                        id
                                )
                                .with(user("analyst")
                                        .roles("ANALYST"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code").value(
                                "NOT_FOUND"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(id)
                );
    }
}
