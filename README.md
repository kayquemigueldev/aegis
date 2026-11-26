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

## Development profiles

AEGIS supports two explicit Spring Boot profiles.

| Profile | Purpose | Authentication default |
|---|---|---|
| `dev` | Local development | `local` |
| `test` | Integration tests | `database` |

To start the backend with the development profile:

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The test profile can be activated with:

```bash
SPRING_PROFILES_ACTIVE=test ./mvnw clean verify
```

**Important:** the `test` profile does not create
an isolated database. Before using it, configure
`DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for a
dedicated test database.

The normal Maven test suite also contains tests
with explicitly selected security modes. Running
the suite against an existing development database
does not provide full test isolation.

## Database configuration

The local Docker Compose project uses PostgreSQL 17.

Default development connection:

- Host: `127.0.0.1`
- Port: `5434`
- Database: `aegis`
- User: `aegis`

The database password must be provided externally.
Never commit `.env` or actual credentials.

Only source a local `.env` file that you have
reviewed and trust.

Flyway manages database migrations. Hibernate
uses `ddl-auto=validate` and must not generate
the database schema automatically.

Do not delete existing Docker volumes while
troubleshooting.

## Troubleshooting

### PostgreSQL unavailable

Check the container status:

```bash
docker compose ps
```

Check whether the PostgreSQL service is healthy
and whether port 5434 is available.

### Authentication or database connection failure

Verify that:

- `POSTGRES_PASSWORD` is configured locally.
- `DB_PASSWORD` matches the database password.
- `DB_URL` points to the intended database.
- `AEGIS_SECURITY_MODE` uses `local` or `database`.

Changing `POSTGRES_PASSWORD` in an environment file
does not automatically change the password stored
in an existing PostgreSQL data volume.

### Flyway validation failure

Review the existing database schema and migration
history. Do not rewrite migrations that have already
been applied or delete volumes to bypass errors.

### Maven or Java failure

Confirm that Java 21 is active:

```bash
java -version
```

Run the Maven Wrapper from the backend directory:

```bash
cd backend
./mvnw --batch-mode clean verify
```

### API access

The development server listens on:

`http://127.0.0.1:8081`

Endpoints are protected by Spring Security.
A response of HTTP 401 or 403 can indicate
authentication or authorization restrictions.

## Verification and limitations

- Java 21 is the supported development baseline.
- PostgreSQL 17 and Flyway migrations V1-V10
  have been exercised in an isolated environment.
- The backend test suite reached 318 passing tests
  in the recorded local verification.
- GitHub Actions runs backend tests automatically.
- OSV-Scanner checks known dependency vulnerabilities.
- The project is not production-ready.
- A fresh-environment HTTP smoke test remains part
  of the separate AEGIS-038 acceptance review.

Further evidence:

- [Spring profiles](docs/infrastructure/spring-profiles.md)
- [Reproducibility](docs/infrastructure/reproducibility.md)
- [Dependency security](docs/infrastructure/dependency-security.md)

## Verified authentication smoke test

An isolated local authentication test was completed
on 2026-11-26.

- Source commit: `b5308c0`.
- Separate PostgreSQL 17 container.
- Backend on localhost port 18082.
- Fresh randomly generated test credentials.
- Unauthenticated API request: HTTP 401.
- Authenticated API request: HTTP 200.
- Temporary services stopped after the test.
- Existing development containers untouched.

This verifies local HTTP Basic authentication
and an authorized API request. It does not
establish production security readiness.

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

## Development operations

- [Architecture](docs/architecture/overview.md)
- [Logging](docs/infrastructure/logging.md)
- [Dependency security](docs/infrastructure/dependency-security.md)
- [Reproducibility](docs/infrastructure/reproducibility.md)
- [Changelog](CHANGELOG.md)

Run verification with PostgreSQL configured:

    cd backend
    ./mvnw --batch-mode clean verify

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
