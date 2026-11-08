
package dev.kayquemiguel.aegis.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aegis.security.mode=database",
        "aegis.bootstrap.admin.username=",
        "aegis.bootstrap.admin.password="
})
@AutoConfigureMockMvc
@Transactional
class AdminUserManagementIntegrationTest {

    private static final String PASSWORD =
            "TestAdminPassword2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    private String createAccount(String role) {
        String username =
                "test-" + UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO aegis_users (
                    id, username, password_hash,
                    role, enabled
                )
                VALUES (?, ?, ?, ?, TRUE)
                """,
                UUID.randomUUID(),
                username,
                encoder.encode(PASSWORD),
                role
        );

        return username;
    }

    private UUID userId(String username) {
        return jdbc.queryForObject(
                """
                SELECT id
                FROM aegis_users
                WHERE username = ?
                """,
                UUID.class,
                username
        );
    }

    @Test
    void shouldRejectAnonymousAdminRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/users")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAnalystAdminRequest()
            throws Exception {

        String analyst = createAccount("ANALYST");

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .with(httpBasic(
                                        analyst,
                                        PASSWORD
                                ))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateAnalystWithHashedPassword()
            throws Exception {

        String admin = createAccount("ADMIN");
        String analyst =
                "new-" + UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "%s",
                                          "password": "NewAnalystPassword2026!",
                                          "sensors": ["sensor-A"]
                                        }
                                        """.formatted(analyst))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.username")
                                .value(analyst)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("ANALYST")
                )
                .andExpect(
                        jsonPath("$.sensors[0]")
                                .value("sensor-A")
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        String storedHash = jdbc.queryForObject(
                """
                SELECT password_hash
                FROM aegis_users
                WHERE username = ?
                """,
                String.class,
                analyst
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                encoder.matches(
                        "NewAnalystPassword2026!",
                        storedHash
                )
        );
    }

    @Test
    void shouldRejectDuplicateUsername()
            throws Exception {

        String admin = createAccount("ADMIN");
        String analyst = createAccount("ANALYST");

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "%s",
                                          "password": "AnotherPassword2026!",
                                          "sensors": []
                                        }
                                        """.formatted(analyst))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectShortPassword()
            throws Exception {

        String admin = createAccount("ADMIN");

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "short-test",
                                          "password": "123",
                                          "sensors": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReplaceAnalystSensors()
            throws Exception {

        String admin = createAccount("ADMIN");
        String analyst = createAccount("ANALYST");
        UUID id = userId(analyst);

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{id}/sensors",
                                id
                        )
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "sensors": ["sensor-B"]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.sensors[0]")
                                .value("sensor-B")
                );
    }

    @Test
    void shouldDisableAnalyst()
            throws Exception {

        String admin = createAccount("ADMIN");
        String analyst = createAccount("ANALYST");
        UUID id = userId(analyst);

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/users/{id}/enabled",
                                id
                        )
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.enabled")
                                .value(false)
                );

        mockMvc.perform(
                        get("/api/v1/alerts")
                                .with(httpBasic(
                                        analyst, PASSWORD
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPreventDisablingAdministrator()
            throws Exception {

        String admin = createAccount("ADMIN");
        UUID id = userId(admin);

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/users/{id}/enabled",
                                id
                        )
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldChangeOwnAdminPassword()
            throws Exception {

        String admin = createAccount("ADMIN");
        String newPassword =
                "UpdatedAdminPassword2026!";

        mockMvc.perform(
                        post("/api/v1/admin/me/password")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "currentPassword": "%s",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(
                                        PASSWORD,
                                        newPassword
                                ))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .with(httpBasic(
                                        admin, newPassword
                                ))
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectIncorrectCurrentPassword()
            throws Exception {

        String admin = createAccount("ADMIN");

        mockMvc.perform(
                        post("/api/v1/admin/me/password")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "currentPassword": "WrongCurrentPassword!",
                                          "newPassword": "UpdatedAdminPassword2026!"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}
