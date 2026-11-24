# AEGIS — Contratos entre módulos

Roadmap: AEGIS-014 e AEGIS-015.

Estado: especificação inicial, sujeita à auditoria.
Este documento descreve contratos existentes e limites
desejados, sem declarar a arquitetura concluída.

## 1. Domain

Pacote: `dev.kayquemiguel.aegis.domain`

Responsável por eventos e tipos de domínio.

Contratos existentes:
- `NetworkEvent`: registro imutável de evento de rede.
- `FlowMetadata`: metadados de fluxo.
- `NetworkProtocol`: protocolo normalizado.
- `AuthenticationEvent`: evento de autenticação.

Regras:
- Não depender de Spring, HTTP ou JPA.
- Validar invariantes dos objetos.
- Não realizar acesso ao banco.
- Não depender dos módulos de ingestão, detecção
  ou segurança.

## 2. Ingestion

Pacote: `dev.kayquemiguel.aegis.ingestion`

Contrato existente:
- `SuricataFlowParser` converte registros suportados
  para o modelo de domínio.
- `SuricataJsonlImporter` lê arquivos JSONL e entrega
  eventos aos consumidores.
- `ImportReport` representa resultados da importação.

Regras desejadas:
- Validar entradas externas.
- Não persistir alertas diretamente.
- Não decidir sobre autorização de usuários.
- Isolar particularidades do formato Suricata.

## 3. Detection

Pacote: `dev.kayquemiguel.aegis.detection`

Contrato existente:

`DetectionRule`:
- `String ruleId()`
- `Optional<DetectionAlert> evaluate(NetworkEvent event)`

`DetectionEngine`:
- Recebe `DetectionRuleRegistry` no construtor.
- Avalia `NetworkEvent`.
- Retorna `List<DetectionAlert>`.
- Expõe `ruleCount()`.

Regras desejadas:
- Identificadores de regra estáveis.
- Nenhuma regra executa código externo arbitrário.
- Regras devem expor resultados explicáveis.
- Mudanças de comportamento devem ter testes.

Limitação:
- O contrato atual ainda não expressa formalmente
  versão de execução e parâmetros reproduzíveis.
- O identificador NET-003 possui divergência
  registrada em `docs/requirements/traceability.md`.

## 4. Processing pipeline

Pacote:
`dev.kayquemiguel.aegis.detection.pipeline`

Contrato existente:
- `SuricataDetectionPipeline.process(Path, String)`
  retorna `DetectionPipelineReport` e pode lançar
  `IOException`.

Responsabilidades:
- Orquestrar importação, detecção e persistência.
- Usar snapshot verificado do arquivo importado.
- Controlar deduplicação por sensor e conteúdo.
- Coordenar transações conforme o processamento.

Limitações:
- O pipeline depende de adaptadores concretos.
- Não existe ainda um conjunto uniforme de portas
  de aplicação para toda persistência.

## 5. Persistence

Pacote:
`dev.kayquemiguel.aegis.detection.persistence`

Responsabilidades:
- Persistência de alertas e eventos suportados.
- Garantias de integridade e idempotência.
- Isolamento dos detalhes de armazenamento.

Regras desejadas:
- Migrações versionadas no Flyway.
- Nenhum acesso direto ao banco pelo domínio.
- Não expor entidades JPA como contrato público HTTP.

## 6. Security

Pacote: `dev.kayquemiguel.aegis.security`

Responsabilidades:
- Autenticação.
- Autorização por papel e sensor.
- Auditoria de operações sensíveis.

Regras:
- Aplicar autorização no servidor.
- Não confiar em sensores informados sem validação.
- Não registrar senhas, tokens ou segredos nos logs.
- Não remover verificações ao refatorar serviços.

## 7. REST API

Pacotes atuais:
- `detection.api`
- `security`

Contratos observados:
- `DetectionAlertController` oferece consulta
  paginada e busca por identificador.
- Paginação possui tamanho máximo de 100.
- Filtros de sensor e regra são validados.
- Respostas usam DTOs em vez de entidades JPA.

Regras desejadas:
- Validar entradas na fronteira HTTP.
- Preservar compatibilidade de contratos publicados.
- Não expor detalhes internos de exceções ou segredos.
- Aplicar autorização antes de retornar dados.

## 8. Dependências autorizadas

| Origem | Destino | Política inicial |
|---|---|---|
| domain | Outros módulos AEGIS | Proibido |
| ingestion | domain | Permitido |
| detection | domain | Permitido |
| detection.pipeline | ingestion | Permitido na orquestração |
| detection.api | security | Permitido para autorização |
| persistence | PostgreSQL | Permitido via adaptadores |
| security | domain | Avaliar caso a caso |

A tabela descreve a direção desejada, não garante
que todas as dependências atuais a respeitem.

## 9. Critérios de aceite AEGIS-015

- [x] Responsabilidades iniciais documentadas.
- [x] Contratos existentes identificados.
- [x] Direções permitidas registradas.
- [ ] Dependências completas revisadas.
- [ ] Regras arquiteturais automatizadas em testes Java.
- [ ] Contratos confirmados por testes de integração.
- [ ] Revisão final e fechamento da issue no GitHub.

## 10. Dívidas técnicas

- Formalizar portas de persistência quando necessário.
- Separar contratos públicos de detalhes de infraestrutura.
- Auditar dependências entre detection e security.
- Corrigir a classificação de identificadores divergentes
  sem alterar registros persistidos silenciosamente.
- Definir política uniforme de erros e correlação.

Referências:
- `docs/architecture/overview.md`
- `docs/architecture/audit-baseline.md`
- `docs/architecture/adr/0001-modular-monolith.md`
