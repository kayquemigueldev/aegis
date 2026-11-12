# ADR-0001: Modular Monolith Architecture

Status: Accepted
Date: 2026-11-12

## Context

AEGIS is a defensive cybersecurity platform
designed to process authorized network telemetry,
detect suspicious behavior, and support security
investigations.

The project requires:

- Maintainability
- Testability
- Security
- Extensibility
- Reproducible processing
- Controlled operational complexity

During its initial development, AEGIS is maintained
by a small development team and does not require
independently deployed services.

## Decision

AEGIS will initially use a modular monolith
architecture.

The primary technology stack is:

- Java 21
- Spring Boot 4.1.2
- Maven
- PostgreSQL 17
- Flyway
- Docker Compose

The backend will organize responsibilities into:

- Domain
- Ingestion
- Detection
- Processing Pipeline
- Persistence
- Security
- REST API

Module boundaries should remain explicit.

Domain logic should not depend directly on
controllers or infrastructure-specific details.

## Rationale

A modular monolith provides:

1. Simpler local development.
2. Lower deployment complexity.
3. Easier debugging and integration testing.
4. Straightforward transactional consistency.
5. Reduced infrastructure overhead.
6. Freedom to evolve internal module boundaries.

Microservices would introduce distributed
communication, deployment orchestration, and
additional operational complexity that is not
currently justified by measured requirements.

## Consequences

### Positive

- Simpler build and deployment.
- Easier end-to-end debugging.
- Lower infrastructure requirements.
- Transactional database operations.
- Faster development iterations.

### Trade-offs

- Modules share a deployment lifecycle.
- Independent scaling is not immediately available.
- Architectural boundaries require discipline.
- Future service extraction may require refactoring.

## Alternatives Considered

### Microservices

Rejected for the initial implementation because
distributed deployment complexity is not currently
justified by project requirements.

### Unstructured Monolith

Rejected because unrestricted coupling would
complicate maintenance, testing, and extension.

## Reconsideration Criteria

The architecture may be reconsidered if:

- Performance benchmarks demonstrate bottlenecks.
- Independent scaling becomes necessary.
- Availability requirements change significantly.
- Specific modules require isolated deployment.
- Team and operational requirements evolve.

Any architecture change must be documented
through a new Architecture Decision Record.

## Related Documentation

- docs/product/vision.md
- docs/architecture/overview.md
- Master Roadmap v1.0
