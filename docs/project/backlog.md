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

