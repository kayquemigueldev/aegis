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
- NET-001: Possible port scan detection
- Detection alert persistence
- Import deduplication and failure recovery
- Database-backed authentication
- ADMIN and ANALYST roles
- Sensor-based access control
- Administrative user management
- Transactional security audit logging
- REST API for querying detection alerts

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
