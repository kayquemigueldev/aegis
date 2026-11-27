# AEGIS — Architecture Baseline

## Scope

Static import audit of production Java sources.
Roadmap coverage: AEGIS-014, 015, 019, 021, 024.

This report is diagnostic evidence, not acceptance
of those roadmap tasks.

## Inventory

Total Java files: **57**

| Module | Java files |
|---|---|
| detection | 37 |
| domain | 5 |
| ingestion | 3 |
| observability | 3 |
| root | 1 |
| security | 8 |

## Cross-module imports

| Source | Target | Import occurrences |
|---|---|---|
| detection | domain | 15 |
| detection | ingestion | 6 |
| detection | security | 2 |
| ingestion | domain | 4 |

## Boundary violations

No violations of the configured initial import rules were detected.

## Architecture observations

Observations are not automatically violations.

| Location | Observation |
|---|---|
| detection/api | REST interfaces reside inside detection module |
| detection/persistence | Persistence adapters reside inside detection module |
| security | Security API, application and infrastructure classes share one package tree |

## Sources missing package declarations

None.

## Limitations

- Imports do not reveal reflection or runtime wiring.
- Fully qualified references inside code may be missed.
- Java source dependencies are not runtime flows.
- API and persistence packages need design review.
- No API contract or error handling audit is included.
- Violations require manual review before refactoring.
- This is not a security penetration test.

## Next architecture activities

1. Review each boundary violation.
2. Formalize module contracts and ownership.
3. Map event, rule, alert and evidence data flows.
4. Define consistent error and correlation policies.
5. Add enforceable architectural regression tests.
