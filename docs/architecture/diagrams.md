# AEGIS — Architecture Diagrams

Roadmap: AEGIS-021.
Status: current-state baseline.

## 1. Modular monolith

```mermaid
flowchart TD
    Client[Authorized API client]
    Security[Spring Security]
    Obs[Correlation ID filter]
    API[REST controllers]
    Pipeline[Detection pipelines]
    Ingest[Suricata ingestion]
    Domain[Normalized domain events]
    Rules[Detection engine and rule registry]
    Persist[Persistence adapters]
    DB[(PostgreSQL 17)]

    Client --> Obs
    Obs --> Security
    Security --> API
    API --> Pipeline
    Pipeline --> Ingest
    Ingest --> Domain
    Pipeline --> Rules
    Rules --> Domain
    Pipeline --> Persist
    API --> Persist
    Persist --> DB
```

This is a conceptual component map, not an exact
Spring dependency or authorization sequence diagram.

## 2. Suricata processing

```mermaid
flowchart TD
    A[Authorized JSONL file]
    B[Verified import snapshot]
    C{Previously imported?}
    D[Suricata JSONL importer]
    E[Suricata flow parser]
    F[NetworkEvent]
    G[DetectionEngine]
    H[Detection rules]
    I[DetectionAlert]
    J[(PostgreSQL)]
    K[Previous import report]

    A --> B
    B --> C
    C -->|Yes| K
    C -->|No| D
    D --> E
    E --> F
    F --> G
    G --> H
    H --> I
    I --> J
    D --> J
```

The pipeline verifies the original source before
completing import processing.

## 3. Auth-event ingestion

```mermaid
flowchart TD
    A[Authenticated HTTP client]
    B[Security and sensor authorization]
    C[AuthenticationEventController]
    D[AuthenticationEvent validation]
    E[Durable authentication processor]
    F[(Event inbox in PostgreSQL)]
    G[Authentication detector]
    H[(Detection alert)]
    I[Recovery scheduler]

    A --> B --> C --> D --> E
    E --> F
    E --> G
    G --> H
    I --> F
    I --> E
```

The diagram summarizes logical responsibilities.
It does not specify every transaction or retry branch.

## 4. Request correlation

```mermaid
flowchart TD
    A[HTTP request]
    B[Correlation ID filter]
    C{Valid canonical UUID?}
    D[Reuse ID]
    E[Generate UUID]
    F[Set MDC and response header]
    G[Spring Security and MVC]
    H[Clear or restore MDC]

    A --> B --> C
    C -->|Yes| D --> F
    C -->|No| E --> F
    F --> G --> H
```

## Limitations

- No Zeek integration is represented as implemented.
- Durable per-event evidence references remain pending.
- Rule execution versioning remains pending.
- Diagrams must be reviewed when integration changes.
