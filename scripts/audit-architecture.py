#!/usr/bin/env python3
"""AEGIS architecture baseline auditor.

Static analysis of source imports. Does not execute Java code.
"""

from pathlib import Path
from collections import Counter, defaultdict
from datetime import date
import argparse
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "backend/src/main/java"
BASE = JAVA / "dev/kayquemiguel/aegis"
REPORT = ROOT / "docs/architecture/audit-baseline.md"

IMPORT_RE = re.compile(
    r"^\s*import\s+(?:static\s+)?"
    r"([a-zA-Z_][\w.]*(?:\.\*)?)\s*;",
    re.MULTILINE
)

PACKAGE_RE = re.compile(
    r"^\s*package\s+([\w.]+)\s*;",
    re.MULTILINE
)

MODULES = (
    "domain",
    "ingestion",
    "detection",
    "security",
)

# Rules represent initial boundaries, not proof of a
# fully implemented clean architecture.
FORBIDDEN = {
    "domain": (
        "org.springframework.",
        "jakarta.persistence.",
        "dev.kayquemiguel.aegis.ingestion.",
        "dev.kayquemiguel.aegis.detection.",
        "dev.kayquemiguel.aegis.security.",
    ),
    "ingestion": (
        "dev.kayquemiguel.aegis.security.",
        "dev.kayquemiguel.aegis.detection.persistence.",
    ),
}

def module_of(package):
    root = "dev.kayquemiguel.aegis."
    if not package.startswith(root):
        return "external"

    suffix = package[len(root):]
    return suffix.split(".", 1)[0]


def category_of(path):
    parts = path.relative_to(BASE).parts
    return parts[0] if len(parts) > 1 else "root"


def render_table(headers, rows):
    lines = [
        "| " + " | ".join(headers) + " |",
        "|" + "|".join("---" for _ in headers) + "|",
    ]
    for row in rows:
        lines.append("| " + " | ".join(
            str(value).replace("|", "\\|")
            for value in row
        ) + " |")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="Compare generated report with committed baseline",
    )
    args = parser.parse_args()

    if not BASE.is_dir():
        sys.exit("ERROR: Java source directory not found")

    sources = sorted(BASE.rglob("*.java"))
    if not sources:
        sys.exit("ERROR: No Java source files found")

    counts = Counter()
    dependencies = Counter()
    violations = []
    observations = []
    missing_packages = []

    for path in sources:
        relative = path.relative_to(ROOT).as_posix()
        content = path.read_text(encoding="utf-8")
        match = PACKAGE_RE.search(content)

        if not match:
            missing_packages.append(relative)
            continue

        package = match.group(1)
        source_module = module_of(package)

        counts[source_module] += 1

        if category_of(path) != source_module:
            observations.append(
                (relative, "Package/directory mismatch")
            )

        imports = IMPORT_RE.findall(content)

        for target in imports:
            destination = module_of(target)

            if (
                destination in MODULES
                and destination != source_module
            ):
                dependencies[
                    (source_module, destination)
                ] += 1

            for prefix in FORBIDDEN.get(
                source_module, ()
            ):
                if target.startswith(prefix):
                    violations.append(
                        (relative, target, prefix)
                    )
                    break

    # Additional architecture observations.
    detection = BASE / "detection"
    security = BASE / "security"

    if (detection / "api").exists():
        observations.append((
            "detection/api",
            "REST interfaces reside inside detection module",
        ))

    if (detection / "persistence").exists():
        observations.append((
            "detection/persistence",
            "Persistence adapters reside inside detection module",
        ))

    if security.exists():
        observations.append((
            "security",
            "Security API, application and infrastructure "
            "classes share one package tree",
        ))

    lines = [
        "# AEGIS — Architecture Baseline",
        "",
        "## Scope",
        "",
        "Static import audit of production Java sources.",
        "Roadmap coverage: AEGIS-014, 015, 019, 021, 024.",
        "",
        "This report is diagnostic evidence, not acceptance",
        "of those roadmap tasks.",
        "",
        "## Inventory",
        "",
        f"Total Java files: **{len(sources)}**",
        "",
        render_table(
            ["Module", "Java files"],
            sorted(counts.items()),
        ),
        "",
        "## Cross-module imports",
        "",
    ]

    if dependencies:
        lines.append(render_table(
            ["Source", "Target", "Import occurrences"],
            [
                (source, target, count)
                for (source, target), count
                in sorted(dependencies.items())
            ],
        ))
    else:
        lines.append("No cross-module imports detected.")

    lines.extend([
        "",
        "## Boundary violations",
        "",
    ])

    if violations:
        lines.append(render_table(
            ["File", "Imported type", "Forbidden prefix"],
            violations,
        ))
    else:
        lines.append(
            "No violations of the configured initial "
            "import rules were detected."
        )

    lines.extend([
        "",
        "## Architecture observations",
        "",
        "Observations are not automatically violations.",
        "",
    ])

    if observations:
        lines.append(render_table(
            ["Location", "Observation"],
            sorted(observations),
        ))
    else:
        lines.append("No additional observations.")

    lines.extend([
        "",
        "## Sources missing package declarations",
        "",
    ])

    if missing_packages:
        lines.extend(f"- `{path}`" for path in missing_packages)
    else:
        lines.append("None.")

    lines.extend([
        "",
        "## Limitations",
        "",
        "- Imports do not reveal reflection or runtime wiring.",
        "- Fully qualified references inside code may be missed.",
        "- Java source dependencies are not runtime flows.",
        "- API and persistence packages need design review.",
        "- No API contract or error handling audit is included.",
        "- Violations require manual review before refactoring.",
        "- This is not a security penetration test.",
        "",
        "## Next architecture activities",
        "",
        "1. Review each boundary violation.",
        "2. Formalize module contracts and ownership.",
        "3. Map event, rule, alert and evidence data flows.",
        "4. Define consistent error and correlation policies.",
        "5. Add enforceable architectural regression tests.",
        "",
    ])

    report = "\n".join(lines)

    print("AEGIS ARCHITECTURE AUDIT")
    print("Java files:", len(sources))
    print("Import rule violations:", len(violations))
    print("Observations:", len(observations))
    print("Missing package declarations:",
          len(missing_packages))

    if args.check:
        if not REPORT.exists():
            sys.exit("ERROR: Baseline report not found")

        current = REPORT.read_text(
            encoding="utf-8"
        )

        if current != report:
            sys.exit(
                "ERROR: Architecture baseline changed. "
                "Regenerate and review the report."
            )

        print("Architecture baseline is current.")
        return

    REPORT.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    REPORT.write_text(
        report,
        encoding="utf-8"
    )

    print("Report:", REPORT.relative_to(ROOT))
    print("Audit completed.")


if __name__ == "__main__":
    main()
