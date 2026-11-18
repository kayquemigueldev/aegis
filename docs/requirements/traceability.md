# Matriz de rastreabilidade

Roadmap: AEGIS-006, AEGIS-007, AEGIS-008.

| Caso de uso | Requisitos | Tarefas do roadmap |
|---|---|---|
| UC-001 | RF-001 a RF-004 | AEGIS-051 a 069 |
| UC-002 | RF-005 a RF-007 | AEGIS-082 a 103 |
| UC-003 | RF-008 a RF-010 | AEGIS-131 a 145, AEGIS-200 |
| UC-004 | RF-011, RF-012 | AEGIS-250 a 262 |
| UC-005 | RF-013, RF-014 | AEGIS-263 a 271 |
| UC-006 | RF-015, RF-016 | AEGIS-158 a 172 |

## Requisitos transversais

- RNF-001, RNF-002: segurança da plataforma.
- RNF-003, RNF-004, RNF-010: integridade e detecção.
- RNF-005, RNF-006: performance.
- RNF-007, RNF-008, RNF-012: operação.
- RNF-009, RNF-011: arquitetura e manutenção.

## Divergências registradas

1. AEGIS-101 foi usado em mensagens de commit para
   recuperação de eventos de autenticação, embora o
   roadmap o reserve para versão e parâmetros da execução.
2. NET-003 foi usado para falhas de autenticação,
   enquanto AEGIS-106 prevê conexões malsucedidas.

Correção futura exige compatibilidade com dados já
persistidos e testes de migração. Não renomear IDs
existentes silenciosamente.
