# AEGIS — Casos de uso

Roadmap: AEGIS-006

## UC-001 — Importar telemetria
Ator: Analista autorizado.
Objetivo: importar registros de uma fonte suportada.
Pré-condições: acesso permitido; fonte identificada.
Fluxo principal:
1. Selecionar fonte e dataset autorizado.
2. Validar tamanho e formato.
3. Normalizar eventos suportados.
4. Registrar aceitos, ignorados e rejeitados.
5. Persistir resultado e checkpoint.
Exceções: arquivo inválido, duplicado, falha de armazenamento.
Pós-condição: importação rastreável e recuperável.
Requisitos: RF-001, RF-002, RF-003, RF-004.

## UC-002 — Executar regras
Ator: Sistema.
Objetivo: avaliar eventos normalizados.
Pré-condições: eventos válidos e regras configuradas.
Fluxo principal:
1. Identificar regras aplicáveis.
2. Aplicar agrupamento e janelas temporais.
3. Avaliar condições de detecção.
4. Registrar resultado, versão e parâmetros.
Exceções: regra inválida, evento atrasado, limite excedido.
Pós-condição: avaliação reproduzível.
Requisitos: RF-005, RF-006, RF-007.

## UC-003 — Consultar alertas
Ator: Analista autorizado.
Objetivo: compreender os motivos de uma detecção.
Pré-condições: sessão autorizada.
Fluxo principal:
1. Consultar alertas com filtros.
2. Selecionar alerta.
3. Visualizar regra, severidade e evidências.
4. Localizar eventos associados.
Exceções: acesso negado, alerta inexistente.
Pós-condição: consulta auditável.
Requisitos: RF-008, RF-009, RF-010.

## UC-004 — Investigar comportamento
Ator: Analista autorizado.
Objetivo: registrar e acompanhar uma investigação.
Pré-condições: autorização e evidências disponíveis.
Fluxo principal:
1. Abrir caso.
2. Associar alertas e eventos.
3. Registrar hipóteses e notas.
4. Atualizar estado e responsável.
5. Produzir relatório.
Exceções: falta de permissão ou referência inválida.
Pós-condição: histórico investigativo preservado.
Requisitos: RF-011, RF-012.

## UC-005 — Administrar acessos
Ator: Administrador.
Objetivo: controlar usuários e permissões.
Pré-condições: autenticação administrativa.
Fluxo principal:
1. Identificar usuário.
2. Validar permissão solicitada.
3. Aplicar alteração.
4. Registrar auditoria.
Exceções: usuário inválido, privilégio insuficiente.
Pós-condição: acesso atualizado e ação auditada.
Requisitos: RF-013, RF-014.

## UC-006 — Reprocessar um experimento
Ator: Pesquisador autorizado.
Objetivo: reproduzir resultados de detecção.
Pré-condições: dataset, regras e versões identificados.
Fluxo principal:
1. Selecionar dataset versionado.
2. Selecionar versão e parâmetros das regras.
3. Executar avaliação isolada.
4. Registrar resultados e diferenças.
Exceções: entrada incompatível ou versão indisponível.
Pós-condição: execução identificável e comparável.
Requisitos: RF-015, RF-016.
