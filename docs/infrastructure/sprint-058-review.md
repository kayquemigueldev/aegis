# Sprint 058 — Final Evidence Review

Date: 2026-11-24
Roadmap: AEGIS-025 to AEGIS-038

VALIDADO means technical evidence exists.
It does not automatically mean CONCLUIDO.

| ID | Evidence status | Basis |
|---|---|---|
| AEGIS-025 | VALIDADO | Repository and origin verified |
| AEGIS-026 | VALIDADO | POM, Maven Wrapper and clean verify |
| AEGIS-027 | VALIDADO | Java 21 backend and HTTP startup |
| AEGIS-028 | VALIDADO | Package structure and architecture audit |
| AEGIS-029 | VALIDADO | PostgreSQL 17 isolated integration |
| AEGIS-030 | VALIDADO | Migrations V1-V10 applied from scratch |
| AEGIS-031 | VALIDADO | Compose configuration and isolated database |
| AEGIS-032 | VALIDADO | Versioned environment template and README |
| AEGIS-033 | VALIDADO | Profile configurations and integration tests |
| AEGIS-034 | VALIDADO | Backend CI on main and pull requests |
| AEGIS-035 | VALIDADO | Dependabot, OSV and security acceptance |
| AEGIS-036 | VALIDADO | README, isolated tests and HTTP 401/200 |
| AEGIS-037 | VALIDADO | Changelog and release policy |
| AEGIS-038 | PARCIAL | Isolated test passed; full acceptance pending |

## CI evidence

- Commit: `55b0f6c`
- GitHub Actions run: `45332254042`
- Conclusion: success
- Local isolated build: 313 tests passed
- Flyway: 10 migrations applied to empty schema

## Important roadmap correction

AEGIS-033 officially requires development and
test profiles, not logging configuration.
Logging documentation remains separate.

## Outstanding acceptance

- Confirm complete criteria for each issue.
- Validate development and test profiles.
- Verify vulnerability alerts and scan results.
- Review README setup from a clean environment.
- Review changelog against release policy.
- Complete reproducibility limitations.

No GitHub issues were automatically closed.

## AEGIS-036 — Final validation

Date: 2026-11-26.

- README installation and execution instructions reviewed.
- Independent clone and PostgreSQL environment verified.
- Flyway migrations applied successfully.
- Maven clean verify: 318 tests, zero failures.
- Protected endpoint without credentials: HTTP 401.
- Protected endpoint with valid credentials: HTTP 200.
- Backend CI and OSV-Scanner passed on `f35d4f4`.

Assessment: VALIDADO.
Backlog acceptance: CONCLUIDO.

AEGIS-038 remains PARCIAL because full
second-machine and cold-cache reproducibility
has not been verified.

## AEGIS-037 — Release documentation review

Date: 2026-11-26.

- Changelog structure reviewed.
- Unreleased section preserved.
- SemVer policy documented.
- Release validation workflow documented.
- No fictitious release or tag created.

Assessment: VALIDADO.
Backlog acceptance: CONCLUIDO.

## Sprint 058 — Consolidated technical acceptance

Date: 2026-11-26.

AEGIS-025 to 034: VALIDADO, subject to formal
GitHub issue acceptance.

AEGIS-035 to 037: closed as completed.

AEGIS-038: PARCIAL.
Full clean-environment reproduction remains pending.

Verified evidence includes:
- 318 passing Java tests.
- PostgreSQL 17 and Flyway V1-V10.
- HTTP 401 and authenticated HTTP 200.
- Backend CI and OSV-Scanner success.
- Architecture baseline with zero import violations.

Remaining limitations:
- Three architecture observations remain.
- Test profile does not isolate database automatically.
- A cold Maven-cache reproduction is not yet verified.
