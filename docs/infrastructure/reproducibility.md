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

## Isolated HTTP smoke test — 2026-11-26

Source commit: `7566c76`.

Environment:
- Independent Git clone in `/tmp`.
- Java 21.0.2.
- Docker 29.7.2.
- Docker Compose 5.5.0 available.
- PostgreSQL 17-alpine in a dedicated container.
- Isolated PostgreSQL port: 55435.
- Isolated backend port: 18081.
- Separate Docker volume.

Verification:
- Independent clone: PASS.
- PostgreSQL readiness: PASS.
- Flyway and Maven clean verify: PASS.
- 318 tests, 0 failures, 0 errors, 0 skipped.
- Spring Boot application startup: PASS.
- Unauthenticated GET /api/v1/alerts: HTTP 401.
- Temporary container stopped after execution.
- Original development containers left untouched.

Evidence logs:
`/tmp/aegis-readme-036.7a2I5B`

Limitations:
- Used existing host Maven dependency cache.
- Only macOS was tested locally.
- HTTP 401 confirms unauthenticated access denial,
  not successful authenticated API access.
- Dedicated Docker volume was preserved for inspection.
- Full second-machine reproducibility remains unverified.

Related roadmap:
- AEGIS-036: installation guide acceptance review.
- AEGIS-038: reproducibility evidence.
