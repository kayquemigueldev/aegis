# Requisitos funcionais — AEGIS

Roadmap: AEGIS-007

| ID | Requisito | Critério de aceite |
|---|---|---|
| RF-001 | Importar telemetria suportada | Dataset válido é processado |
| RF-002 | Validar entrada | Dados inválidos são rejeitados com motivo |
| RF-003 | Emitir relatório de importação | Totais aceitos, rejeitados e ignorados são verificáveis |
| RF-004 | Recuperar importação interrompida | Repetição não duplica resultados indevidamente |
| RF-005 | Executar regras de detecção | Eventos são avaliados pelas regras aplicáveis |
| RF-006 | Aplicar parâmetros e janelas | Configuração inválida é rejeitada |
| RF-007 | Registrar execução | Versão e parâmetros permitem reprodução |
| RF-008 | Consultar alertas | Listagem oferece filtros e paginação |
| RF-009 | Exibir evidências | Alerta identifica os eventos relacionados |
| RF-010 | Controlar acesso aos alertas | Usuário não consulta sensores proibidos |
| RF-011 | Abrir casos investigativos | Caso recebe identificador e responsável |
| RF-012 | Manter histórico de investigação | Mudanças e evidências permanecem rastreáveis |
| RF-013 | Gerenciar usuários e permissões | Ação exige autorização administrativa |
| RF-014 | Auditar operações sensíveis | Operação registra ator, ação e resultado |
| RF-015 | Executar experimentos offline | Dataset e configuração definidos produzem resultado comparável |
| RF-016 | Comparar execuções | Diferenças entre versões são identificadas |

Esta especificação define comportamento esperado.
A presença do requisito não comprova implementação.
