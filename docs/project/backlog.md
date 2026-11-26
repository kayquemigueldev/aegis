# Backlog e controle do roadmap

Roadmap: AEGIS-010
Fonte de verdade: Master Roadmap v1.0 — 364 tarefas.

Estados:
- CONCLUIDO: implementação, teste e evidência confirmados.
- PARCIAL: implementação/documentação incompleta.
- PENDENTE: ausente ou não comprovado.

## Sprint 056 — Fundação documental

| ID | Entrega | Estado |
|---|---|---|
| AEGIS-004 | Escopo de funcionalidades | PARCIAL |
| AEGIS-006 | Casos de uso | PARCIAL |
| AEGIS-007 | Requisitos funcionais | PARCIAL |
| AEGIS-008 | Requisitos não funcionais | PARCIAL |
| AEGIS-009 | Versionamento | PARCIAL |
| AEGIS-010 | Backlog e milestones | CONCLUIDO |
| AEGIS-011 | Indicadores de sucesso | PARCIAL |
| AEGIS-012 | Riscos e dependências | PARCIAL |

Estados PARCIAL são intencionais: a documentação
gerada precisa ser revisada e aprovada antes do
fechamento oficial.

## Próximas dependências
1. Conferir requisitos contra todos os módulos.
2. Completar arquitetura e diagramas.
3. Organizar issues e milestones reais no GitHub.
4. Corrigir divergências de IDs sem perda histórica.
5. Auditar os critérios da release v0.1.0.

A lista integral AEGIS-001 a AEGIS-364 permanece
definida no Master Roadmap v1.0.


## Evidence register

Evidence below supports the existence of implementations.
It does not automatically establish full acceptance.

| Roadmap ID | Repository evidence | Assessment |
|---|---|---|
| AEGIS-013 | docs/architecture/adr/0001-modular-monolith.md | Architecture decision documented |
| AEGIS-025 | GitHub repository kayquemigueldev/aegis | Repository exists |
| AEGIS-026 | backend/pom.xml and Maven wrapper | Maven configured |
| AEGIS-027 | backend/src/main/java/dev/kayquemiguel/aegis/AegisApplication.java | Spring Boot application |
| AEGIS-029 | compose.yml | PostgreSQL configured |
| AEGIS-030 | backend/src/main/resources/db/migration/ | Flyway migrations V1-V10 |
| AEGIS-031 | compose.yml | Docker Compose configured |
| AEGIS-032 | .env.example | Partial environment example |
| AEGIS-034 | .github/workflows/backend-ci.yml | Automated backend build and tests |
| AEGIS-082 | backend/src/main/java/dev/kayquemiguel/aegis/detection/DetectionRule.java | Rule interface exists |
| AEGIS-083 | backend/src/main/java/dev/kayquemiguel/aegis/detection/DetectionRuleRegistry.java | Registry exists |
| AEGIS-084 | backend/src/main/java/dev/kayquemiguel/aegis/detection/DetectionEngine.java | Basic executor exists |
| AEGIS-104 | backend/src/main/java/dev/kayquemiguel/aegis/detection/PossiblePortScanDetector.java | NET-001 implemented; acceptance review pending |
| AEGIS-105 | backend/src/main/java/dev/kayquemiguel/aegis/detection/HorizontalPortScanDetector.java | NET-002 implemented; acceptance review pending |

## Traceability debt

- R-009: roadmap AEGIS-101 was used in commits for
  durable authentication recovery.
- R-009: NET-003 refers to authentication failures in
  the implementation, but unsuccessful connections
  in the original Master Roadmap.
- AEGIS-010 remains PARTIAL until GitHub issues
  and milestones are created and verified.
- Release v0.1.0 remains unverified.

## Governance gate

Before closing a roadmap task:

1. Verify the official ID and acceptance criterion.
2. Record implementation or document evidence.
3. Verify appropriate automated tests.
4. Record unresolved limitations.
5. Update the backlog and traceability matrix.
6. Confirm CI when implementation has changed.


## Sprint 056 — GitHub backlog sincronizado

- Milestone M01: https://github.com/kayquemigueldev/aegis/milestone/1
- Issues oficiais AEGIS-001 a AEGIS-038 criadas.
- Sincronizador: scripts/sync-github-roadmap.py
- Criacao confirmada em 24/11/2026.
- AEGIS-010: criterios de backlog inicial atendidos.
- As demais tarefas permanecem sujeitas a auditoria.

## Sprint 057 — Revisao de arquitetura

Revisao: `docs/architecture/sprint-057-review.md`.

| Roadmap | Estado | Evidencia |
|---|---|---|
| AEGIS-014 | PARCIAL | 5285e75 |
| AEGIS-015 | PARCIAL | 40d1caa; 2c98415 |
| AEGIS-016 | PARCIAL | fbd4731 |
| AEGIS-017 | PARCIAL | fbd4731 |
| AEGIS-018 | PARCIAL | fbd4731 |
| AEGIS-019 | PARCIAL | 533da45 |
| AEGIS-020 | PARCIAL | 533da45; 0ea36fa |
| AEGIS-021 | PARCIAL | 0ea36fa |
| AEGIS-022 | PARCIAL | 0ea36fa |
| AEGIS-023 | PARCIAL | 0ea36fa |
| AEGIS-024 | PARCIAL | 0ea36fa |

Nenhuma tarefa deste bloco foi automaticamente
encerrada. As limitacoes e proximas verificacoes
estao documentadas no relatorio da sprint.

## Sprint 058 — Infrastructure evidence review

Review: `docs/infrastructure/sprint-058-review.md`.

- AEGIS-025 to AEGIS-038 audited.
- CI success confirmed for commit `55b0f6c`.
- Isolated PostgreSQL test completed successfully.
- AEGIS-033 corrected to development/test profiles.
- Remaining acceptance limitations documented.
- No issues automatically closed.

## AEGIS-035 — Security pipeline validation

Estado: CONCLUIDO.

Evidence:
- `docs/infrastructure/dependency-security.md`
- PR #44 merged as `6dff78b`
- OSV scanner successful on PR and main
- Backend CI successful on PR and main
- First full scan: no issues found

Acceptance:
- Dependabot alerts reviewed: zero open.
- Code scanning reviewed: zero open.
- Security updates enabled (owner-confirmed).
- PR #44 and main security checks passed.
- Technical acceptance recorded on 2026-11-26.

Ongoing: monitor and triage future security findings.

## AEGIS-036 — README installation review

Estado: CONCLUIDO.

Evidence:
- README installation and execution guide
- Java 21 and PostgreSQL 17 prerequisites
- Development and test profile instructions
- Database configuration and troubleshooting
- Local Markdown link validation

Acceptance:
- Isolated clone and PostgreSQL setup validated.
- Flyway and 318 tests completed successfully.
- Backend startup and HTTP 401 verified.
- Authenticated HTTP 200 verified.
- Final README guidance reviewed.
- CI and OSV-Scanner passed on `f35d4f4`.

Technical acceptance: 2026-11-26.
GitHub issue closure remains a separate action.

## AEGIS-036/038 — Isolated smoke test evidence

Date: 2026-11-26.

AEGIS-036: CONCLUIDO — isolated installation and
authenticated API smoke tests passed.

AEGIS-038: PARCIAL — independent clone, PostgreSQL,
318 tests and HTTP startup validated. Maven cache
and second-machine limitations remain.

Evidence:
- `docs/infrastructure/reproducibility.md`
- Commit tested: `7566c76`
- HTTP 401 on protected endpoint
- Original Docker services preserved

No GitHub issues automatically closed.


## AEGIS-036 — Authentication acceptance evidence

Date: 2026-11-26.
Estado: CONCLUIDO — technical acceptance approved.

Verified:
- Isolated clone and PostgreSQL startup.
- Flyway migrations and 318 successful tests.
- Backend startup on isolated HTTP port.
- Unauthenticated request: HTTP 401.
- Authenticated request: HTTP 200.
- Original development services preserved.

Evidence:
- README.md
- docs/infrastructure/reproducibility.md
- Local logs: /tmp/aegis-auth-036.UakB1k

Acceptance:
- README installation instructions reviewed.
- Backend CI and OSV-Scanner passed on `f35d4f4`.
- Technical acceptance documented in the backlog.
- Formal GitHub issue closure to follow.
