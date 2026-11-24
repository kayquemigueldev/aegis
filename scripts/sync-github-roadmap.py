#!/usr/bin/env python3
"""AEGIS Master Roadmap v1.0 - GitHub backlog sync.

Default: dry-run.
Use --apply to create missing GitHub milestone/issues.
Existing issues are preserved.
"""

import argparse
import json
import re
import subprocess
import sys

REPO = "kayquemigueldev/aegis"
MILESTONE = "M01 — Fundação e arquitetura"

TASKS = {
    1: "Criar o documento de visão do produto",
    2: "Definir o público-alvo inicial",
    3: "Definir os problemas que o AEGIS pretende resolver",
    4: "Definir funcionalidades obrigatórias e opcionais",
    5: "Documentar o que está fora do escopo da versão 1.0",
    6: "Definir casos de uso principais",
    7: "Documentar requisitos funcionais",
    8: "Documentar requisitos não funcionais",
    9: "Estabelecer o modelo de versionamento",
    10: "Criar o backlog inicial e organizar milestones no GitHub",
    11: "Definir indicadores de sucesso para o projeto",
    12: "Registrar riscos e dependências técnicas",
    13: "Definir a arquitetura de monólito modular",
    14: "Separar domínio, aplicação, infraestrutura e interfaces",
    15: "Definir os contratos entre os módulos",
    16: "Projetar o modelo de eventos normalizados",
    17: "Projetar o modelo de regras de detecção",
    18: "Projetar o modelo de alertas e evidências",
    19: "Definir tratamento de erros e exceções",
    20: "Definir padrões de logging e identificadores de correlação",
    21: "Criar diagramas de componentes e fluxos de dados",
    22: "Documentar decisões arquiteturais com ADRs",
    23: "Definir estratégia para configuração dos módulos",
    24: "Definir mecanismos de extensibilidade sem executar plugins não confiáveis",
    25: "Criar o repositório GitHub",
    26: "Inicializar projeto Maven",
    27: "Configurar o backend Spring Boot",
    28: "Definir estrutura de diretórios e módulos",
    29: "Configurar PostgreSQL",
    30: "Integrar Flyway",
    31: "Configurar Docker Compose",
    32: "Criar variáveis de ambiente de exemplo",
    33: "Configurar perfis de desenvolvimento e testes",
    34: "Configurar GitHub Actions para build e testes",
    35: "Configurar verificação de dependências vulneráveis",
    36: "Criar README de instalação e execução",
    37: "Criar CHANGELOG e política de releases",
    38: "Garantir execução reproduzível em ambiente limpo",
}

assert len(TASKS) == 38
assert list(TASKS) == list(range(1, 39))


def gh(*args):
    result = subprocess.run(
        ["gh", *args],
        capture_output=True,
        text=True,
        check=False,
    )

    if result.returncode:
        raise RuntimeError(
            f"GitHub command failed:\n"
            f"{result.stderr.strip()}"
        )

    return result.stdout


def api_json(endpoint, *args):
    return json.loads(
        gh("api", endpoint, *args)
    )


def task_id(number):
    return f"AEGIS-{number:03d}"


def section(number):
    if number <= 12:
        return "Planejamento do produto"
    if number <= 24:
        return "Arquitetura"
    return "Infraestrutura"


def task_body(number, title):
    code = task_id(number)

    extra = ""

    if number == 10:
        extra = (
            "\n## Evidência específica\n"
            "Validar a criação real de issues e milestones, "
            "sem duplicar IDs ou tarefas.\n"
        )

    return f"""## Tarefa oficial

**ID:** {code}
**Roadmap:** AEGIS Master Roadmap v1.0
**Parte:** I — Fundação e arquitetura
**Área:** {section(number)}

## Objetivo

{title}.

## Critérios de aceite

- [ ] Entrega corresponde ao escopo original.
- [ ] Artefatos ou código vinculados à issue.
- [ ] Evidências e limitações documentadas.
- [ ] Testes ou verificações aplicáveis realizados.
- [ ] Rastreabilidade atualizada no backlog.

## Estado inicial

**Pendente de validação formal.**

A existência de documentação ou código não
significa que a tarefa foi concluída.

## Referências

- `docs/product/vision.md`
- `docs/project/backlog.md`
- `docs/requirements/traceability.md`

{extra}

> Não reutilizar este ID para outra funcionalidade.
"""


parser = argparse.ArgumentParser()
parser.add_argument(
    "--apply",
    action="store_true",
    help="Create missing GitHub resources"
)
args = parser.parse_args()

print("AEGIS - Master Roadmap v1.0")
print("Repository:", REPO)
print("Mode:", "APPLY" if args.apply else "DRY RUN")

# Validate GitHub access before performing any writes.
gh("auth", "status")
repo = api_json(f"repos/{REPO}")

if repo["full_name"].lower() != REPO.lower():
    raise RuntimeError("Unexpected repository")

existing_milestones = api_json(
    f"repos/{REPO}/milestones?state=all&per_page=100"
)

milestone = next(
    (
        item for item in existing_milestones
        if item["title"] == MILESTONE
    ),
    None
)

if milestone:
    milestone_number = milestone["number"]
    print("Existing milestone:", milestone_number)
elif args.apply:
    milestone = api_json(
        f"repos/{REPO}/milestones",
        "-X", "POST",
        "-f", f"title={MILESTONE}",
        "-f", (
            "description=AEGIS-001 through AEGIS-038. "
            "Foundation and architecture, November 2026."
        )
    )
    milestone_number = milestone["number"]
    print("Created milestone:", milestone_number)
else:
    milestone_number = None
    print("Would create milestone:", MILESTONE)

# GitHub Issues endpoint also returns PRs; exclude those.
existing = api_json(
    f"repos/{REPO}/issues?state=all&per_page=100"
)

known = {}

for item in existing:
    if "pull_request" in item:
        continue

    match = re.match(
        r"^\[(AEGIS-\d{3})\]",
        item["title"]
    )

    if match:
        known[match.group(1)] = item["number"]

created = 0
skipped = 0

for number, title in TASKS.items():
    code = task_id(number)

    if code in known:
        print(f"SKIP {code}: #{known[code]}")
        skipped += 1
        continue

    issue_title = f"[{code}] {title}"

    if not args.apply:
        print("WOULD CREATE:", issue_title)
        continue

    issue = api_json(
        f"repos/{REPO}/issues",
        "-X", "POST",
        "-f", f"title={issue_title}",
        "-f", f"body={task_body(number, title)}",
        "-F", f"milestone={milestone_number}",
    )

    print(f"CREATED {code}: {issue['html_url']}")
    created += 1

print()
print("Created:", created)
print("Already present:", skipped)
print("Expected official IDs:", len(TASKS))

if not args.apply:
    print("Dry run complete. No GitHub changes made.")
else:
    print("GitHub sync complete.")
    print("Re-run safely to check existing tasks.")
