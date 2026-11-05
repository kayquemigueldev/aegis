
package dev.kayquemiguel.aegis.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aegis.security.username=security-test",
        "aegis.security.password=test-password-123456"
})
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    private static final String USERNAME =
            "security-test";

    private static final String PASSWORD =
            "test-password-123456";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectAnonymousRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/alerts")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        header().exists(
                                "WWW-Authenticate"
                        )
                );
    }

    @Test
    void shouldRejectInvalidPassword()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(
                                        httpBasic(
                                                USERNAME,
                                                "invalid-password"
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedAnalyst()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(
                                        httpBasic(
                                                USERNAME,
                                                PASSWORD
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content").isArray()
                )
                .andExpect(
                        jsonPath("$.page").value(0)
                );
    }

    @Test
    void shouldRejectUserWithoutAnalystRole()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(
                                        user("viewer")
                                                .roles("VIEWER")
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyWriteRequests()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/alerts")
                                .with(
                                        httpBasic(
                                                USERNAME,
                                                PASSWORD
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForAlertById()
            throws Exception {

        UUID id = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/alerts/{id}",
                                id
                        )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get(
                                "/api/v1/alerts/{id}",
                                id
                        ).with(
                                httpBasic(
                                        USERNAME,
                                        PASSWORD
                                )
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyUnknownEndpoints()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/unknown")
                                .with(
                                        httpBasic(
                                                USERNAME,
                                                PASSWORD
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }
}
