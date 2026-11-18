# AEGIS

**Network Threat Detection & Security Intelligence Platform**

A defensive cybersecurity platform developed with Java 21
and Spring Boot for analyzing authorized network telemetry,
detecting suspicious behavior, and supporting investigations.

> Status: Under development. Not production-ready.

## Technology Stack

- Java 21
- Spring Boot 4.1.2
- Maven
- PostgreSQL 17
- Flyway
- Docker Compose
- Spring Security
- JUnit 5

## Implemented Features

- Suricata EVE JSONL ingestion
- Network flow normalization
- NET-001: Possible TCP port scan detection
- NET-002: Horizontal port scan detection
- Generic DetectionRule interface, registry and engine
- Authentication failure detector (identifier under review)
- Detection alert persistence
- Import deduplication and failure recovery
- Database-backed authentication
- ADMIN and ANALYST roles
- Sensor-based access control
- Administrative user management
- Transactional security audit logging
- REST API for querying detection alerts

## Development governance

The official Master Roadmap v1.0 contains 364 tasks.
Roadmap IDs must never be reused for unrelated features.

Current planning documents:

- [Product vision](docs/product/vision.md)
- [Feature scope](docs/product/feature-scope.md)
- [Use cases](docs/product/use-cases.md)
- [Functional requirements](docs/requirements/functional.md)
- [Non-functional requirements](docs/requirements/non-functional.md)
- [Traceability](docs/requirements/traceability.md)
- [Versioning](docs/project/versioning.md)
- [Backlog](docs/project/backlog.md)
- [Success metrics](docs/project/success-metrics.md)
- [Risk register](docs/project/risk-register.md)

The authentication detector currently uses NET-003,
which conflicts with the original roadmap definition.
The identifier requires a compatibility-safe correction.

A documentation file does not prove a roadmap task
has been completed. Implementation, tests and evidence
are required before closing the task.

## Requirements

- Java 21
- Docker and Docker Compose
- Git

## Getting Started

Clone the repository:

    git clone https://github.com/kayquemigueldev/aegis.git

    cd aegis

Configure the environment:

    cp .env.example .env

Edit .env and set a strong POSTGRES_PASSWORD.

Start PostgreSQL:

    docker compose up -d --wait

Load the database password:

    set -a
    source .env
    export DB_PASSWORD="$POSTGRES_PASSWORD"
    set +a

Start the backend:

    cd backend
    ./mvnw spring-boot:run

API address:

    http://127.0.0.1:8081

## Running Tests

With PostgreSQL running and DB_PASSWORD configured:

    cd backend
    ./mvnw clean test

## Architecture

The project follows a modular monolith approach.

Main areas:

- Domain
- Ingestion
- Detection
- Persistence
- Security
- REST API

See docs/architecture/overview.md.

## Security

AEGIS is intended for authorized environments.

Current local development uses HTTP Basic authentication.
Do not expose the development server directly to the
public internet.

Detection alerts represent potentially suspicious
behavior, not definitive proof of compromise.

## Roadmap

The Master Roadmap contains 364 tracked tasks.

Target stable release: April 2028.

## Author

Kayque Miguel

GitHub: https://github.com/kayquemigueldev
