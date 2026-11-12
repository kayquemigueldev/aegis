# AEGIS - Architecture Overview

## Architectural Style

AEGIS follows a modular monolith architecture.

The backend uses Java 21 and Spring Boot 4.1.2.

The architecture prioritizes:

- Separation of responsibilities
- Testability
- Security
- Maintainability
- Extensibility
- Reproducible detection behavior

## Main Components

### 1. Domain

Package:

dev.kayquemiguel.aegis.domain.event

Main classes:

- NetworkEvent
- NetworkProtocol
- FlowMetadata

Responsibilities:

- Represent normalized network telemetry
- Validate essential event fields
- Preserve source and sensor identification
- Model immutable network events

### 2. Ingestion

Package:

dev.kayquemiguel.aegis.ingestion.suricata

Main classes:

- SuricataFlowParser
- SuricataJsonlImporter
- ImportReport

Responsibilities:

- Read Suricata EVE JSONL
- Parse supported flow records
- Validate event formats
- Reject invalid records
- Report import results

### 3. Detection

Package:

dev.kayquemiguel.aegis.detection

Main classes:

- PossiblePortScanDetector
- DetectionAlert

Responsibilities:

- Analyze normalized events
- Detect suspicious network behavior
- Apply time-based detection logic
- Generate explainable detection alerts

Current detection rule:

NET-001 - Possible TCP Port Scan

Future:

- Generic DetectionRule interface
- Central detection rule registry
- Configurable detection parameters
- Multiple independent detection rules
- Detection-as-Code

### 4. Processing Pipeline

Package:

dev.kayquemiguel.aegis.detection.pipeline

Main classes:

- SuricataDetectionPipeline
- ImportRunRepository
- VerifiedImportSnapshot
- SuricataImportRunner

Responsibilities:

- Coordinate event ingestion and detection
- Validate imported file snapshots
- Prevent duplicate import processing
- Persist import reports
- Coordinate alert persistence

### 5. Persistence

Database:

PostgreSQL 17

Migration system:

Flyway

Existing migrations:

- V1 - Schema metadata
- V2 - Detection alerts
- V3 - Import runs
- V4 - Security users
- V5 - Security audit log

Responsibilities:

- Persist detection alerts
- Track imported files
- Store security users and permissions
- Store administrative audit records

### 6. Security

Package:

dev.kayquemiguel.aegis.security

Main components:

- ApiSecurityConfig
- DatabaseUserDetailsService
- SensorAccessPolicy
- AdminUserService
- SecurityAuditService
- SecurityAuditController

Responsibilities:

- Authenticate API users
- Enforce ADMIN and ANALYST roles
- Restrict access by sensor
- Manage administrative operations
- Audit sensitive administrative actions

### 7. REST API

Current API capabilities:

- Detection alert queries
- Detection alert details
- Administrative user management
- Administrative security audit queries

The API is protected by Spring Security.

## Current Data Flow

Authorized Suricata EVE JSONL
            |
            v
SuricataJsonlImporter
            |
            v
SuricataFlowParser
            |
            v
NetworkEvent
            |
            v
PossiblePortScanDetector
            |
            v
DetectionAlert
            |
            v
PostgreSQL
            |
            v
Secured REST API

## Future Frontend Architecture

Planned technologies:

- React
- TypeScript
- Vite

Planned modules:

- Security Overview
- Event Explorer
- Detection Center
- Alert Workspace
- Investigations
- Sensor Management
- Administration
- Security Audit

The frontend will communicate with the
Spring Boot backend through secured REST APIs.

## Current Limitations

- The detection pipeline currently uses NET-001.
- A generic detection rule registry is pending.
- Out-of-order detection is not fully supported.
- Zeek ingestion is not implemented.
- Full event evidence persistence is pending.
- The web console is not implemented.
- Production security hardening remains pending.

## Architectural Principles

1. Keep the domain independent of HTTP interfaces.
2. Avoid unnecessary distributed complexity.
3. Validate untrusted telemetry.
4. Maintain explicit authorization boundaries.
5. Preserve detection traceability.
6. Prefer deterministic and testable algorithms.
7. Document significant architectural decisions.

## Architecture Decision Records

Significant architectural decisions are documented
in docs/architecture/adr/.

The first decision is ADR-0001,
which establishes the modular monolith approach.
