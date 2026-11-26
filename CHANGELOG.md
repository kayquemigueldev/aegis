# Changelog

All notable changes to AEGIS are documented here.

The project is under active development.
No stable release has been published.

## [Unreleased]

### Added

- Java 21 and Spring Boot backend.
- PostgreSQL 17 integration using Docker Compose.
- Flyway database migrations V1 through V10.
- Immutable network event model.
- Suricata EVE JSONL flow ingestion.
- Network event validation and normalization.
- NET-001 possible TCP port scan detection.
- NET-002 horizontal port scan detection.
- DetectionRule interface, registry and engine.
- Authentication failure detection and durable inbox.
- Authentication replay recovery and idempotent alerts.
- Detection alert persistence and query API.
- Import deduplication using SHA-256 and sensor ID.
- Tests for import failures and recovery.
- HTTP Basic authentication.
- Database-backed user accounts.
- ADMIN and ANALYST authorization.
- Sensor-scoped alert access.
- Administrative user management.
- Transactional security audit trail.
- JUnit 5 automated regression tests.

### Documentation

- Initial project README.
- Product vision and architecture overview.
- ADR-0001 establishing the modular monolith.
- Backend CI workflow running Java 21 and PostgreSQL 17.
- Sprint 056: product scope, use cases, requirements,
  traceability, versioning, risk register and backlog.

### Known Limitations

- Generic rule engine exists, but advanced execution
  controls and versioned rule runs remain incomplete.
- Authentication detection currently uses NET-003,
  conflicting with the original roadmap definition.
  Identifier migration must preserve persisted history.
- Zeek ingestion is not yet supported.
- React/TypeScript console is not implemented.
- Investigation workflows remain planned.
- Production security hardening is incomplete.

## Development Milestones

### Sprint 021 - Security Audit Trail

- Added Flyway migration V5.
- Implemented transactional administrative auditing.
- Added paginated administrative audit queries.
- Added security audit integration tests.
- Verified authenticated audit API access.

Commit: 97e0dbf

### Sprint 020 - Administrative Security

- Expanded administrative user management.
- Added authentication and authorization tests.
- Established the foundation for security auditing.

Commit: b94c659

## Changelog maintenance

Roadmap: AEGIS-037.

- Record notable changes under `Unreleased`.
- Group changes by Added, Changed, Fixed,
  Security and Removed when applicable.
- Describe changes from a user or operator
  perspective rather than copying commit logs.
- At release time, move applicable entries
  into a dated version section.
- Preserve the history of earlier releases.
- Never claim a release exists solely because
  its target version appears in the roadmap.

The current changelog describes development
work. No stable release is declared.
