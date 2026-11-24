# AEGIS — Reproducibility

Roadmap: AEGIS-038.
Status: verification pending.

## Clean environment checklist

- [ ] Clone the repository into a new directory.
- [ ] Verify Java 21 and Docker Compose.
- [ ] Create a separate development environment.
- [ ] Configure unique credentials.
- [ ] Use an isolated PostgreSQL volume and port.
- [ ] Apply Flyway migrations from scratch.
- [ ] Execute Maven clean verify.
- [ ] Verify backend startup.
- [ ] Confirm no secrets were committed.
- [x] Confirm GitHub Actions passes.

Never delete the existing development database
or execute docker compose down -v against it.

## Verified isolated execution

Date: 2026-11-24

Environment:
- Fresh independent Git working copy.
- Separate Docker Compose project.
- PostgreSQL 17.11 on localhost port 55434.
- Dedicated Docker volume.
- Java 21 and Maven Wrapper.

Results:
- PostgreSQL container reached healthy state.
- Flyway applied migrations V1 through V10
  to a previously empty schema.
- Maven clean verify completed successfully.
- 313 tests passed.
- 0 failures, 0 errors, 0 skipped.
- Executable Spring Boot JAR was generated.

Limitations:
- Dependencies used the existing Mac's Maven cache.
- No separate HTTP server smoke test was performed.
- A second operating system was not tested.
- GitHub Actions success confirmed for 55b0f6c (run 45332254042).

Acceptance:
AEGIS-038 has a successful isolated local integration
test. Full formal acceptance remains pending.
