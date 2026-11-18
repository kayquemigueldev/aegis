from pathlib import Path
from collections import Counter
import re
import sys

root = Path(__file__).resolve().parents[1]
errors = []

files = {
    "RF": root / "docs/requirements/functional.md",
    "RNF": root / "docs/requirements/non-functional.md",
    "UC": root / "docs/product/use-cases.md",
}

catalog = {}

for prefix, path in files.items():
    content = path.read_text(encoding="utf-8")

    if prefix == "UC":
        ids = re.findall(
            r"^## (UC-\d{3})\b",
            content,
            flags=re.MULTILINE
        )
    else:
        ids = re.findall(
            rf"^\| ({prefix}-\d{{3}}) \|",
            content,
            flags=re.MULTILINE
        )

    duplicates = [
        key for key, count in Counter(ids).items()
        if count > 1
    ]

    if duplicates:
        errors.append(
            f"Duplicate {prefix}: {duplicates}"
        )

    if not ids:
        errors.append(f"No {prefix} identifiers found")

    catalog[prefix] = set(ids)

traceability = (
    root / "docs/requirements/traceability.md"
).read_text(encoding="utf-8")

for uc in re.findall(r"\bUC-\d{3}\b", traceability):
    if uc not in catalog["UC"]:
        errors.append(f"Unknown use case: {uc}")

for prefix in ("RF", "RNF"):
    references = re.findall(
        rf"\b{prefix}-(\d{{3}})(?:\s+a\s+{prefix}-(\d{{3}}))?",
        traceability
    )

    for first, last in references:
        start = int(first)
        end = int(last) if last else start

        for number in range(start, end + 1):
            identifier = f"{prefix}-{number:03d}"

            if identifier not in catalog[prefix]:
                errors.append(
                    f"Unknown requirement: {identifier}"
                )

readme = (root / "README.md").read_text(
    encoding="utf-8"
)

for destination in re.findall(
    r"\]\((docs/[^)#]+\.md)\)",
    readme
):
    if not (root / destination).is_file():
        errors.append(
            f"Broken README link: {destination}"
        )

if errors:
    print("ROADMAP DOCUMENT VALIDATION FAILED")
    for error in errors:
        print("ERROR:", error)
    sys.exit(1)

print("ROADMAP DOCUMENT VALIDATION PASSED")
print(f"Use cases: {len(catalog['UC'])}")
print(f"Functional requirements: {len(catalog['RF'])}")
print(f"Non-functional requirements: {len(catalog['RNF'])}")
