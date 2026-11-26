# AEGIS — Dependency Security

Roadmap: AEGIS-035.
Status: CONCLUIDO — technical acceptance recorded.

Dependabot monitors Maven and GitHub Actions
dependencies weekly.

## Review process

1. Review security alerts and affected versions.
2. Assess the impact on AEGIS.
3. Apply compatible fixes.
4. Run the full test suite.
5. Confirm CI before merging.

Dependabot version updates do not replace a
dedicated vulnerability scan.

Security alerts depend on repository settings.

## OSV-Scanner integration

Roadmap: AEGIS-035.

Configured workflows:

- osv-scanner-pr.yml
- osv-scanner-scheduled.yml

The scanner checks known dependency
vulnerabilities in the backend Maven project.

Execution:
- On pull requests targeting main.
- On pushes to main.
- Weekly on Monday.
- Manually through GitHub Actions.

The full scan uses fail-on-vuln=true.

## Security acceptance criteria

- [x] Dependabot configuration committed.
- [x] OSV-Scanner workflows prepared.
- [x] GitHub Actions recognizes both workflows.
- [x] First full scan completes.
- [x] First scan results reviewed; no issues found.
- [x] Current alert status reviewed and documented.
- [x] No false positives reported in reviewed scan; future findings require justification.

Scanner findings must not be silently ignored.
Do not disable security checks merely to
obtain a successful build.

A successful scan does not establish that
the application has no vulnerabilities.

## First verified OSV-Scanner execution

Date: 2026-11-25 (America/Sao_Paulo)
Commit: `0e4f418`
GitHub Actions run: `45744922335`

Results:
- Workflow completed successfully.
- Scanner processed `backend/pom.xml`.
- Initial scan reported 13 packages.
- Scanner reporter: `No issues found`.
- SARIF upload completed successfully.
- Backend CI also completed successfully.

Limitations:
- Results reflect the dependencies and advisory
  database available at scan time.
- Pull request workflow still needs validation.
- GitHub security settings require review.
- Future scan findings must be triaged.

AEGIS-035 remains pending final acceptance.


## Pull request validation

This documentation-only pull request verifies
that dependency security automation executes
on pull requests targeting main.

No application dependencies or runtime
configuration are modified.

## Pull request and main validation

PR: https://github.com/kayquemigueldev/aegis/pull/44
PR commit: `dd7b3fc`
Merge commit: `6dff78b`

Verified workflow executions:

- PR OSV: `45745783769` — success
- PR Backend CI: `45745783519` — success
- Main OSV: `45746022674` — success
- Main Backend CI: `45746022401` — success

The PR introduced documentation changes only.

The first full scan previously reported
`No issues found`.

Remaining:
- Review GitHub Dependabot alerts.
- Review Code Scanning alerts and repository settings.
- Record findings or confirm no open alerts.
- Decide formal issue acceptance.

Do not classify an inaccessible alerts page
as evidence that no vulnerabilities exist.

## GitHub security settings verification

Date: 2026-11-26.

Verified in repository settings:
- Dependency graph: enabled.
- Dependabot alerts: enabled.
- Dependabot alerts: 0 open at inspection.
- Code scanning: 0 open alerts at inspection.
- OSV-Scanner: successful on main and PR #44.

Dependabot security updates should be verified
separately after enabling the setting.

No open alerts at inspection does not guarantee
the absence of vulnerabilities.

## AEGIS-035 — Final technical acceptance

Date: 2026-11-26.
Issue: https://github.com/kayquemigueldev/aegis/issues/35

Acceptance evidence:
- Dependency graph enabled.
- Dependabot alerts enabled.
- Dependabot security updates enabled
  (confirmed by repository owner).
- Dependabot alerts: zero open at inspection.
- Code scanning: zero open at inspection.
- OSV-Scanner validated on main and PR #44.
- PR #44 merged successfully.
- Latest verified commit: `7ec7eac`.
- Backend CI run `45746688574`: success.
- OSV run `45746688610`: success.
- Maven suite: 318 tests passed locally.

Outcome: CONCLUIDO.

Remaining operational responsibilities:
- Review new security alerts as they arrive.
- Investigate any failed security scans.
- Validate security update pull requests.
- Reassess dependencies after updates.

This acceptance establishes that vulnerability
scanning is configured and verified. It does
not certify that all dependencies are secure.
