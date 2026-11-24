# AEGIS — Domain contracts baseline

Roadmap: AEGIS-016, AEGIS-017, AEGIS-018.
Status: partial, verified through explicit contract tests.

## AEGIS-016 — Normalized events

### NetworkEvent

Existing immutable Java record with:

- UUID event identifier
- Occurrence and ingestion timestamps
- Source and destination IP addresses
- Optional transport ports
- Network protocol
- Source system and source flow identifier
- Optional flow metadata
- Sensor identity

The record validates required fields and IP addresses.

### AuthenticationEvent

Separate immutable record for authentication telemetry:

- UUID event identifier
- Occurrence and ingestion timestamps
- Source IP address
- Username
- Source system
- Sensor identity
- Authentication outcome

Authentication failures are distinguishable through
`isFailure()`.

### Gaps

- No unified versioned event envelope exists yet.
- Event provenance and evidence-retention policies
  require further specification.
- Cross-source ordering and timestamp guarantees
  remain under review.

## AEGIS-017 — Detection rules

### Existing contracts

`DetectionRule` exposes:

- `String ruleId()`
- `Optional<DetectionAlert> evaluate(NetworkEvent event)`

`DetectionRuleRegistry` rejects duplicate or invalid IDs.

`DetectionEngine` evaluates registered rules and returns
an immutable list of alerts.

`DetectionEngineFactory` creates independent engines
from rule factories.

### Gaps

- Rule version and configuration snapshot are not
  represented in the current DetectionRule interface.
- Reproducible execution metadata is still required.
- NET-003 is historically overloaded with authentication
  failures, conflicting with the original roadmap.
- Do not silently rename persisted rule identifiers.

## AEGIS-018 — Alerts and evidence

### Existing network alert fields

- Rule ID
- Source and destination IP addresses
- Detection timestamp
- Observed count
- Detection threshold
- Description
- Sensor ID
- Metric type

### Existing persistence fields

`DetectionAlertEntity` additionally supplies a UUID,
creation timestamp and authentication-specific metadata.

### Evidence limitations

The current alert stores detection metadata, but does
not yet contain a complete, durable set of source-event
evidence references.

A later version must establish:

1. Evidence identity and event provenance.
2. Relationships between an alert and its source events.
3. Rules for retention, access control and integrity.
4. Reproducible execution/version metadata.
5. Backward-compatible persistence migrations.
6. Tests for missing, duplicated and unauthorized evidence.

No evidence persistence is claimed in this sprint.

## Acceptance state

- [x] Existing event models inventoried.
- [x] Existing rule contracts inventoried.
- [x] Existing alert persistence mapped.
- [x] Selected invariants covered by contract tests.
- [ ] Event envelope and provenance reviewed.
- [ ] Versioned rule execution model approved.
- [ ] Durable evidence model implemented and tested.
- [ ] Full roadmap acceptance criteria verified.

These tasks remain PARTIAL until the missing contracts
and implementations are completed.
