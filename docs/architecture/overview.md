# AEGIS — Architecture Overview

## Architectural style

AEGIS currently follows a modular monolith model,
documented in ADR-0001.

Technology baseline:
- Java 21 and Spring Boot 4.1.2
- Maven and JUnit
- PostgreSQL 17
- Flyway
- Docker Compose
- Spring Security
- ArchUnit

## Current components

| Module | Responsibilities |
|---|---|
| domain.event | Network and authentication events |
| ingestion.suricata | JSONL parsing and normalization |
| detection | Rules, registry, engine and alerts |
| detection.pipeline | Import, processing and recovery |
| detection.persistence | Alert persistence and inbox |
| detection.api | Protected alert and ingestion APIs |
| security | Authentication, authorization, administration |
| observability | Correlation IDs and HTTP error responses |

A single module may contain application and
infrastructure responsibilities today. Additional
separation remains a roadmap activity.

## Detection rules

Current network detection capabilities include:
- NET-001: possible port scan
- NET-002: horizontal port scan

The existing authentication failure detector uses
NET-003 in the code. This conflicts with the official
roadmap, which reserves NET-003 for anomalous
unsuccessful network connections.

Do not silently rename historical rule IDs.

`DetectionRule`, `DetectionRuleRegistry`,
`DetectionEngine` and `DetectionEngineFactory`
already exist.

## Processing

Suricata ingestion uses JSONL parsing, validated
network events, import deduplication and alert
persistence.

Authentication-event ingestion has separate
processing and durable recovery mechanisms.

See `diagrams.md` for component and flow diagrams.

## Persistence

PostgreSQL 17 with Flyway migrations V1 through V10.

Migration coverage includes schema metadata,
alerts, imports, security, audit logging, metrics,
authentication events and idempotency support.

Existing migrations must never be rewritten after
application to shared or persistent databases.

## API and security

Spring Security protects alert queries,
authentication-event ingestion and admin endpoints.

Authorization uses roles and sensor access controls.

The observability module provides:
- X-Correlation-ID headers
- SLF4J MDC correlation context
- Standardized responses for selected MVC errors

Authentication failures are handled by Spring
Security and currently preserve their HTTP status.

## Known limitations

- Durable source-event evidence linkage is incomplete.
- Versioned rule-execution metadata is incomplete.
- NET-003 identifier reconciliation is pending.
- Zeek ingestion is not implemented.
- A full web console is not implemented.
- Correlation propagation to async tasks is pending.
- Production hardening is not complete.

## Architecture documentation

- `module-contracts.md`
- `domain-contracts.md`
- `audit-baseline.md`
- `diagrams.md`
- `configuration-contracts.md`
- `extensibility.md`
- `adr/0001-modular-monolith.md`
- `adr/0002-durable-detection-processing.md`
- `adr/0003-http-correlation.md`

Roadmap AEGIS-021 to AEGIS-024 remains subject to
final acceptance review.
