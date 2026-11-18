# Registro de riscos e dependências

Roadmap: AEGIS-012

Escala qualitativa:
Probabilidade: baixa, média, alta.
Impacto: baixo, médio, alto.

| ID | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R-001 | Alertas falsos positivos | Alta | Médio | Casos benignos e limiares calibrados |
| R-002 | Eventos atrasados ou fora de ordem | Média | Alto | Janelas, tolerâncias e testes |
| R-003 | Duplicação após recuperação | Média | Alto | Idempotência e testes de falha |
| R-004 | Saturação por entrada excessiva | Média | Alto | Limites e benchmarks |
| R-005 | Vazamento de credenciais | Média | Alto | Variáveis e revisão de segurança |
| R-006 | Falha na restauração de dados | Média | Alto | Exercícios de backup/restore |
| R-007 | Incompatibilidade entre regras | Média | Médio | Contratos e testes de integração |
| R-008 | Documentação desatualizada | Alta | Médio | Revisão por sprint |
| R-009 | Divergência entre IDs e roadmap | Alta | Alto | Matriz e política de identificação |

## Dependências principais
- Java 21 e Maven Wrapper.
- PostgreSQL 17 e Flyway.
- Docker Compose.
- Dados de laboratório autorizados.
- CI GitHub Actions.
- Datasets confiáveis para avaliar qualidade.
- Documentação de compatibilidade de APIs e banco.

As classificações são avaliações iniciais de risco,
não resultados de incidentes ou medições reais.
