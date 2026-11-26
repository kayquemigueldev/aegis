# Política de versionamento

Roadmap: AEGIS-009

Formato: SemVer — MAJOR.MINOR.PATCH.

## Marcos planejados
- v0.1.0: CLI, ingestão básica e primeira detecção.
- v0.2.0: normalização e importação confiáveis.
- v0.3.0: motor de regras e janelas temporais.
- v0.4.0: biblioteca inicial de detecções.
- v0.5.0: alertas, evidências, correlação e persistência.
- v0.6.0: API e console inicial.
- v0.7.0: investigações, autenticação e autorização.
- v0.8.0: ingestão contínua e Zeek.
- v0.9.0: pesquisa, inteligência e qualidade.
- v1.0.0-rc: estabilização, segurança e benchmarks.
- v1.0.0: release estável documentada.

## Regras
1. Versões somente após validação dos critérios.
2. PATCH: correção compatível.
3. MINOR: novas capacidades compatíveis.
4. MAJOR: mudanças incompatíveis após estabilidade.
5. Alterações de banco exigem migração versionada.
6. Alterações em APIs exigem avaliação de compatibilidade.
7. Changelog atualizado a cada release.
8. Nenhum release é declarado sem evidência de CI.

Estado atual: desenvolvimento; release v0.1.0
ainda não auditada como concluída.

## Release workflow

Roadmap: AEGIS-037.

1. Select a version based on the roadmap
   and semantic versioning policy.
2. Verify the intended release scope and
   acceptance criteria.
3. Run the automated tests and review CI.
4. Review migrations, configuration changes,
   compatibility and known limitations.
5. Review dependency security findings.
6. Update CHANGELOG.md: move relevant entries
   from Unreleased to the versioned section.
7. Record the release date and known limitations.
8. Create the Git tag and GitHub release only
   after the validation evidence is approved.
9. Preserve links to the release commit,
   checks and documented evidence.

Release candidate versions must be explicitly
distinguished from stable releases.

Do not publish releases automatically as part
of this documentation task.

## AEGIS-037 acceptance evidence

Date: 2026-11-26.

Artifacts:
- CHANGELOG.md
- docs/project/versioning.md
- docs/project/backlog.md

Scope: documented changelog maintenance and
release procedure for future versions.

Limitation: no new release or Git tag was
created or validated in this task.
