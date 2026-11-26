# AEGIS — Dependency Security

Roadmap: AEGIS-035.
Status: scanner integration pending CI validation.

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
- [ ] GitHub Actions recognizes both workflows.
- [ ] First full scan completes.
- [ ] Results and findings are reviewed.
- [ ] Alerts are triaged and documented.
- [ ] False positives are justified if present.

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
