# AEGIS — Sprint 059: Revisão da AEGIS-014

Data: 2026-11-27.

Issue: https://github.com/kayquemigueldev/aegis/issues/14

Branch: `feature/aegis-014-architecture`

## Objetivo

Separar responsabilidades de domínio, aplicação,
infraestrutura e interfaces no monólito modular AEGIS.

## Implementações

- Modelos de leitura independentes dos DTOs HTTP.
- Porta `DetectionAlertReadPort`.
- Adaptador `JpaDetectionAlertReadAdapter`.
- `DetectionAlertQueryService` em `detection.application`.
- Autorização de consultas de alertas por papel e sensor.
- Regras ArchUnit para as fronteiras entre camadas.
- Atualização do baseline de arquitetura.

## Commits da Sprint

- `34b6cdf` — Detection engine boundaries.
- `7ab8815` — Detection core boundaries.
- `fe33218` — Alert response decoupling.
- `4b77e08` — Read port and JPA adapter.
- `c720553` — Persistence boundary test.
- `24d75f6` — Sensor authorization test.
- `69b4c3e` — Analyst role enforcement.
- `6cd9d1a` — Application layer query service.
- `62d474c` — Application and API boundaries.
- `cde5414` — Architecture baseline refresh.

## Evidências locais

Comando: `./mvnw clean verify`.

Ambiente: Java 21, PostgreSQL 17 isolado
em `127.0.0.1:55436`.

Resultado:
- 324 testes executados.
- Zero falhas.
- Zero erros.
- Zero testes ignorados.
- BUILD SUCCESS.

Auditoria:
- 57 arquivos Java.
- Zero violações das regras de importação configuradas.
- Três observações arquiteturais.
- Zero declarações de pacote ausentes.
- Baseline atualizado e aprovado com `--check`.

## Limitações registradas

1. As interfaces REST continuam em `detection/api`.
2. Os adaptadores JPA continuam em `detection/persistence`.
3. O módulo `security` ainda combina responsabilidades
   de diferentes camadas.
4. A camada de aplicação utiliza componentes
   de Spring Data e Spring Security.
5. A aplicação ainda lança exceções HTTP.
6. A porta de leitura utiliza `Page` e `Pageable`.
7. O auditor estático não verifica todas as
   dependências ou o comportamento em execução.
8. O GitHub Actions da branch ainda precisa
   ser verificado.

## Estado

Implementação e validação local concluídas.

Aceitação formal pendente de:
- Pull Request.
- Verificação do CI.
- Revisão dos critérios de aceite.
- Decisão de encerramento da issue.

Não encerrar automaticamente a AEGIS-014.
