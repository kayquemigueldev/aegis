# Changelog

All notable changes to AEGIS are documented here.

The project is under active development.
No stable release has been published.

## [Unreleased]

### Added

- Java 21 and Spring Boot backend.
- PostgreSQL 17 integration using Docker Compose.
- Flyway database migrations V1 through V5.
- Immutable network event model.
- Suricata EVE JSONL flow ingestion.
- Network event validation and normalization.
- NET-001 possible TCP port scan detection.
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
- Product vision and architecture documentation
  planned for Sprint 022.
- Backend CI workflow planned for Sprint 022.

### Known Limitations

- Generic detection rule engine is not yet implemented.
- Only NET-001 detection is available.
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
