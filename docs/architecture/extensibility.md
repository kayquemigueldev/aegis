# AEGIS — Extensibility Policy

Roadmap: AEGIS-024.
Status: initial extension guidelines.

## Adding a detection rule

1. Review the official roadmap task identifier.
2. Reserve a unique rule ID.
3. Identify supported normalized event types.
4. Implement the appropriate detection contract.
5. Prefer deterministic and explainable outcomes.
6. Validate configurable thresholds.
7. Test valid, invalid and boundary inputs.
8. Include tests for duplicate events and ordering.
9. Document alert metrics and evidence requirements.
10. Register the rule in the appropriate engine factory.

The current `DetectionRule` interface accepts
`NetworkEvent` and returns `Optional<DetectionAlert>`.

It is not a generic contract for every event type.
Authentication detection has separate implementation
contracts.

## Adding an ingestion adapter

1. Define the input source and trust boundary.
2. Validate input format and size limits.
3. Normalize into existing domain event types.
4. Keep parser code independent of alert persistence.
5. Preserve event source and sensor identity.
6. Define deduplication and failure behavior.
7. Add malformed-input and integration tests.

No Zeek ingestion adapter is claimed as implemented.

## Preserving compatibility

- Do not silently change rule identifiers.
- Do not mutate existing Flyway migrations.
- Use new migrations for persistent schema changes.
- Preserve API authorization and response contracts.
- Record architecture decisions when boundaries change.
- Run ArchUnit and all integration tests.

## Known conflicts

`NET-003` currently identifies an authentication
failure detector in existing code, but the original
roadmap assigns it to anomalous network connections.

Reconciliation requires explicit compatibility
analysis and a planned migration if needed.

## Definition of done for an extension

- Source code reviewed.
- Unit and integration tests passed.
- Security boundary reviewed.
- Documentation updated.
- Roadmap traceability maintained.
- CI successful.
