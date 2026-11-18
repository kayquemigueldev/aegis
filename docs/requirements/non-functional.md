# Requisitos não funcionais — AEGIS

Roadmap: AEGIS-008

| ID | Requisito | Validação |
|---|---|---|
| RNF-001 | Segurança de credenciais | Sem segredos no Git; configuração externa |
| RNF-002 | Controle de acesso | Testes negativos de autorização |
| RNF-003 | Integridade de eventos | Testes de duplicação, erro e recuperação |
| RNF-004 | Determinismo | Mesmo dataset e configuração geram resultados equivalentes |
| RNF-005 | Limites de memória | Testes de carga e medição de heap |
| RNF-006 | Desempenho observável | Medir throughput, latência média, p95 e p99 |
| RNF-007 | Disponibilidade diagnosticável | Health/readiness e logs de falha |
| RNF-008 | Reprodutibilidade | Instalação e testes em ambiente limpo |
| RNF-009 | Extensibilidade segura | Regras não executam código arbitrário |
| RNF-010 | Rastreabilidade | Alertas identificam regra, versão e evidência |
| RNF-011 | Manutenibilidade | Testes automatizados e ADRs |
| RNF-012 | Recuperabilidade | Backup e restauração testados |

Metas quantitativas de desempenho serão calibradas
após benchmarks reproduzíveis, não presumidas.
