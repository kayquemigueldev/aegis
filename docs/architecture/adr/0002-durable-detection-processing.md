# ADR-0002: Durable detection processing

Status: Accepted as documentation of current direction
Date: 2026-11-24

## Context

Network and authentication telemetry require reliable
processing without accidentally duplicating results.

Existing implementation includes:
- Verified Suricata import snapshots.
- Import-run deduplication.
- An authentication event inbox.
- Retry and startup recovery mechanisms.

## Decision

Maintain processing orchestration in the modular
monolith with PostgreSQL-backed state.

Preserve idempotency and explicit failure handling.
Do not introduce a message broker without an
identified operational or throughput requirement.

## Consequences

- Processing can be recovered using durable state.
- Data consistency can use local transactions.
- Application and database availability remain coupled.
- More explicit replay and evidence contracts are needed.

## Follow-up

- Document per-stage failure semantics.
- Formalize versioned rule execution metadata.
- Test replay and crash recovery behavior.
- Reconcile historical AEGIS-101 ID reuse.
