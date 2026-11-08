
package dev.kayquemiguel.aegis.security;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SecurityAuditService {

    private static final Set<String> ACTIONS = Set.of(
            "USER_CREATED",
            "USER_SENSORS_UPDATED",
            "USER_STATUS_UPDATED",
            "ADMIN_PASSWORD_CHANGED"
    );

    private final JdbcTemplate jdbc;

    public SecurityAuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record AuditEvent(
            UUID id,
            String actorUsername,
            String action,
            UUID targetUserId,
            String targetUsername,
            String details,
            OffsetDateTime occurredAt
    ) {
    }

    public record AuditPage(
            List<AuditEvent> content,
            int page,
            int size,
            long totalElements
    ) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            String action,
            UUID targetUserId,
            String targetUsername,
            String details
    ) {
        if (!ACTIONS.contains(action)) {
            throw new IllegalArgumentException(
                    "Unsupported audit action"
            );
        }

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || authentication.getAuthorities()
                .stream()
                .noneMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                )) {
            throw new IllegalStateException(
                    "Authenticated administrator required "
                            + "for audit recording"
            );
        }

        jdbc.update(
                """
                INSERT INTO aegis_security_audit_log (
                    id,
                    actor_username,
                    action,
                    target_user_id,
                    target_username,
                    details
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                authentication.getName(),
                action,
                targetUserId,
                targetUsername,
                details
        );
    }

    @Transactional(readOnly = true)
    public AuditPage findEvents(
            int page,
            int size,
            String actor,
            String action
    ) {
        if (page < 0 || page > 10000
                || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination parameters"
            );
        }

        if (actor != null
                && (actor.isBlank()
                || actor.length() > 64
                || !actor.matches(
                "[A-Za-z0-9][A-Za-z0-9._-]*"
        ))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid actor filter"
            );
        }

        if (action != null && !ACTIONS.contains(action)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid audit action"
            );
        }

        StringBuilder where =
                new StringBuilder(" WHERE 1 = 1");

        List<Object> arguments = new ArrayList<>();

        if (actor != null) {
            where.append(" AND actor_username = ?");
            arguments.add(actor);
        }

        if (action != null) {
            where.append(" AND action = ?");
            arguments.add(action);
        }

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) "
                        + "FROM aegis_security_audit_log"
                        + where,
                Long.class,
                arguments.toArray()
        );

        String query = """
                SELECT
                    id,
                    actor_username,
                    action,
                    target_user_id,
                    target_username,
                    details,
                    occurred_at
                FROM aegis_security_audit_log
                """
                + where
                + """
                 ORDER BY occurred_at DESC, id DESC
                 LIMIT ? OFFSET ?
                """;

        List<Object> pageArguments =
                new ArrayList<>(arguments);

        pageArguments.add(size);
        pageArguments.add(page * size);

        List<AuditEvent> events = jdbc.query(
                query,
                (rs, rowNum) -> new AuditEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("actor_username"),
                        rs.getString("action"),
                        rs.getObject(
                                "target_user_id",
                                UUID.class
                        ),
                        rs.getString("target_username"),
                        rs.getString("details"),
                        rs.getObject(
                                "occurred_at",
                                OffsetDateTime.class
                        )
                ),
                pageArguments.toArray()
        );

        return new AuditPage(
                events,
                page,
                size,
                count == null ? 0L : count
        );
    }
}
