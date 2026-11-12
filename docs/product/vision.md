# AEGIS - Product Vision

## Product Name

AEGIS - Network Threat Detection &
Security Intelligence Platform

## Vision

Develop a defensive cybersecurity platform capable
of processing authorized network telemetry,
identifying suspicious behaviors, and supporting
evidence-based security investigations.

## Target Audience

- Cybersecurity students
- Software developers studying defensive security
- Security analysts working in authorized labs
- Researchers evaluating network detection methods

## Problems to Solve

1. Network logs are difficult to analyze manually.
2. Suspicious patterns can be hidden among benign events.
3. Security alerts often lack clear explanations.
4. Detection experiments can be difficult to reproduce.
5. Analysts need traceable evidence for investigations.

## Core Functional Requirements

- Ingest and normalize network telemetry.
- Support Suricata and, eventually, Zeek.
- Execute configurable threat detection rules.
- Detect suspicious network behaviors.
- Generate and persist security alerts.
- Preserve alert evidence and provenance.
- Provide secured REST APIs.
- Support user roles and sensor-level permissions.
- Record sensitive administrative operations.
- Provide a web-based Security Operations Console.
- Support investigation workflows.
- Enable reproducible detection experiments.

## Non-Functional Requirements

- Modular and maintainable architecture.
- Secure handling of credentials and sensitive data.
- Deterministic detection behavior where applicable.
- Automated unit and integration testing.
- Controlled memory and resource usage.
- Reproducible development environments.
- Traceable and explainable detection results.
- Documented performance limitations.

## Initial Technical Architecture

- Java 21
- Spring Boot
- Maven
- PostgreSQL 17
- Flyway
- Docker
- JUnit 5

Planned frontend:

- React
- TypeScript
- Vite

Architectural style:

- Modular monolith

## Current Implemented Capabilities

- Suricata EVE JSONL flow ingestion
- Network event normalization and validation
- NET-001 possible port scan detection
- Persistent detection alerts
- Import deduplication
- Database-backed authentication
- ADMIN and ANALYST authorization
- Sensor-based access control
- Administrative security auditing

## Out of Scope

- Unauthorized network surveillance
- Offensive exploitation
- Automatic retaliation against suspected attackers
- Executing untrusted detection plugins
- Claims of infallible threat detection
- Production deployment without security validation

## Success Criteria

AEGIS should eventually allow authorized users to:

1. Import network telemetry.
2. Normalize and inspect network events.
3. Execute documented detection rules.
4. Review alerts and their supporting evidence.
5. Investigate suspicious network activity.
6. Reproduce detection experiments.
7. Operate the platform securely.

## Development Roadmap

The Master Roadmap contains 364 tracked tasks.

Development is organized into incremental sprints.

Target release: v1.0.0 in April 2028.

Completion requires implementation, testing,
documentation, and validation against the roadmap.

## Project Principles

- Security by design
- Evidence before conclusions
- Explainable detections
- Reproducible experiments
- Incremental development
- No unverified completion claims
