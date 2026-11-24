# AEGIS — Configuration Contracts

Roadmap: AEGIS-023.
Status: current configuration inventory.

## Application settings

| Property | Environment variable | Description |
|---|---|---|
| spring.datasource.url | DB_URL | PostgreSQL JDBC URL |
| spring.datasource.username | DB_USERNAME | Database user |
| spring.datasource.password | DB_PASSWORD | Database credential |
| aegis.security.mode | AEGIS_SECURITY_MODE | local or database |
| aegis.security.username | AEGIS_API_USERNAME | Legacy local API user |
| aegis.security.password | AEGIS_API_PASSWORD | Legacy local API password |
| aegis.security.sensors | AEGIS_API_SENSORS | Authorized local sensors |
| aegis.bootstrap.admin.username | AEGIS_BOOTSTRAP_ADMIN_USERNAME | Optional initial admin |
| aegis.bootstrap.admin.password | AEGIS_BOOTSTRAP_ADMIN_PASSWORD | Optional initial admin credential |

The current development server binds to 127.0.0.1
on port 8081.

## Configuration rules

1. Do not commit real credentials.
2. Configure secrets with environment variables or
   an approved secret manager.
3. Validate identifiers and required settings.
4. Keep database migrations under Flyway.
5. Review changes to authentication modes.
6. Never use logging configuration to expose secrets.
7. Document new environment variables before use.

## Observability

The HTTP correlation filter uses the MDC key
`correlationId`.

The response header is `X-Correlation-ID`.

The logging pattern includes this MDC key, showing a
fallback value when no HTTP context exists.

## Pending

- Structured JSON logging for production.
- Deployment-specific configuration documentation.
- Secret rotation procedures.
- Validation tests covering configuration profiles.
- Formal configuration contracts for future adapters.
