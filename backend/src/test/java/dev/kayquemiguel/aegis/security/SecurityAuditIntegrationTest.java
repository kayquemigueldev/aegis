
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

import static org.junit.jupiter.api.Assertions.*;
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
class SecurityAuditIntegrationTest {

    private static final String PASSWORD =
            "AuditAdminPassword2026!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    private String account(String role) {
        String username =
                "audit-" + UUID.randomUUID();

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

    private String uniqueUsername() {
        return "new-" + UUID.randomUUID();
    }

    private String createBody(String username) {
        return """
                {
                  "username": "%s",
                  "password": "AuditAnalystPassword2026!",
                  "sensors": ["sensor-A"]
                }
                """.formatted(username);
    }

    private void createAnalyst(
            String admin,
            String username
    ) throws Exception {
        mvc.perform(
                post("/api/v1/admin/users")
                        .with(httpBasic(admin, PASSWORD))
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(createBody(username))
        ).andExpect(status().isCreated());
    }

    private long auditCount(
            String action,
            String targetUsername
    ) {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM aegis_security_audit_log
                WHERE action = ?
                  AND target_username = ?
                """,
                Long.class,
                action,
                targetUsername
        );

        return count == null ? 0 : count;
    }

    @Test
    void shouldRejectAnonymousAuditAccess()
            throws Exception {

        mvc.perform(
                get("/api/v1/admin/audit")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAnalystAuditAccess()
            throws Exception {

        String analyst = account("ANALYST");

        mvc.perform(
                get("/api/v1/admin/audit")
                        .with(httpBasic(
                                analyst,
                                PASSWORD
                        ))
        ).andExpect(status().isForbidden());
    }

    @Test
    void shouldRecordAdministratorAndTarget()
            throws Exception {

        String admin = account("ADMIN");
        String analyst = uniqueUsername();

        createAnalyst(admin, analyst);

        mvc.perform(
                        get("/api/v1/admin/audit")
                                .with(httpBasic(
                                        admin, PASSWORD
                                ))
                                .param("actor", admin)
                                .param(
                                        "action",
                                        "USER_CREATED"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].actorUsername"
                        ).value(admin)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].targetUsername"
                        ).value(analyst)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].action"
                        ).value("USER_CREATED")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].occurredAt"
                        ).exists()
                );

        assertEquals(
                1,
                auditCount("USER_CREATED", analyst)
        );
    }

    @Test
    void shouldRejectInvalidAuditFilters()
            throws Exception {

        String admin = account("ADMIN");

        mvc.perform(
                get("/api/v1/admin/audit")
                        .with(httpBasic(
                                admin, PASSWORD
                        ))
                        .param("size", "101")
        ).andExpect(status().isBadRequest());

        mvc.perform(
                get("/api/v1/admin/audit")
                        .with(httpBasic(
                                admin, PASSWORD
                        ))
                        .param(
                                "action",
                                "UNKNOWN_ACTION"
                        )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void shouldAuditSensorAndStatusUpdates()
            throws Exception {

        String admin = account("ADMIN");
        String analyst = uniqueUsername();

        createAnalyst(admin, analyst);

        UUID analystId = jdbc.queryForObject(
                """
                SELECT id
                FROM aegis_users
                WHERE username = ?
                """,
                UUID.class,
                analyst
        );

        mvc.perform(
                put(
                        "/api/v1/admin/users/{id}/sensors",
                        analystId
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
        ).andExpect(status().isOk());

        mvc.perform(
                patch(
                        "/api/v1/admin/users/{id}/enabled",
                        analystId
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
        ).andExpect(status().isOk());

        assertEquals(
                1,
                auditCount(
                        "USER_SENSORS_UPDATED",
                        analyst
                )
        );

        assertEquals(
                1,
                auditCount(
                        "USER_STATUS_UPDATED",
                        analyst
                )
        );
    }


    @Test
    void shouldNotAuditRejectedCreation()
            throws Exception {

        String admin = account("ADMIN");
        String analyst = uniqueUsername();

        createAnalyst(admin, analyst);

        // Rejeição por validação, sem provocar
        // uma falha SQL na transação do teste.
        mvc.perform(
                post("/api/v1/admin/users")
                        .with(httpBasic(
                                admin, PASSWORD
                        ))
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                            {
                              "username": "invalid-user",
                              "password": "123",
                              "sensors": ["sensor-A"]
                            }
                            """)
        ).andExpect(status().isBadRequest());

        // A criação válida continua registrada
        // exatamente uma vez na auditoria.
        assertEquals(
                1,
                auditCount("USER_CREATED", analyst)
        );

        // Nenhum evento da operação rejeitada.
        assertEquals(
                0,
                auditCount(
                        "USER_CREATED",
                        "invalid-user"
                )
        );
    }

}
