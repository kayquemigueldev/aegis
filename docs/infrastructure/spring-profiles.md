# AEGIS — Spring Boot Profiles

Roadmap: AEGIS-033.
Status: implementation pending full acceptance.

## Profiles

### Default

Existing shared configuration in
`application.properties`.

Retained for compatibility with current tests.

### Development

`application-dev.properties`

Activation:

`SPRING_PROFILES_ACTIVE=dev`

Local authentication is the default unless
AEGIS_SECURITY_MODE overrides it.

### Tests

`application-test.properties`

Activation:

`SPRING_PROFILES_ACTIVE=test`

Database authentication is the default unless
AEGIS_SECURITY_MODE overrides it.

This profile does not provision a fresh database.
Always point DB_URL to the intended test database.

## Security

- No real credentials in versioned properties.
- DB_PASSWORD remains required externally.
- Flyway remains enabled.
- Hibernate schema management remains validate.
- Do not run integration tests against production data.

## Verification

ProfileConfigurationTest verifies baseline properties.

Pending acceptance:
- Run profile-specific Spring context smoke tests.
- Verify integration tests under the test profile.
- Review CI activation strategy.
- Confirm compatibility and document evidence.

## Profile activation integration tests

The following tests verify Spring context startup:

- DevProfileIntegrationTest
- TestProfileIntegrationTest

They assert the active profile and selected
configuration properties.

Database integration uses externally supplied
credentials. These tests do not independently
provision an isolated database.

The main test suite must also pass before acceptance.
