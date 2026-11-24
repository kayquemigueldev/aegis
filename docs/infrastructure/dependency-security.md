# AEGIS — Dependency Security

Roadmap: AEGIS-035.
Status: initial configuration.

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
