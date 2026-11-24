# AEGIS — Reproducibility

Roadmap: AEGIS-038.
Status: verification pending.

## Clean environment checklist

- [ ] Clone the repository into a new directory.
- [ ] Verify Java 21 and Docker Compose.
- [ ] Create a separate development environment.
- [ ] Configure unique credentials.
- [ ] Use an isolated PostgreSQL volume and port.
- [ ] Apply Flyway migrations from scratch.
- [ ] Execute Maven clean verify.
- [ ] Verify backend startup.
- [ ] Confirm no secrets were committed.
- [ ] Confirm GitHub Actions passes.

Never delete the existing development database
or execute docker compose down -v against it.
