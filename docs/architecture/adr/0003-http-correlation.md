# ADR-0003: HTTP request correlation

Status: Accepted as current implementation baseline
Date: 2026-11-24

## Context

Operational investigations require matching HTTP
responses to server-side diagnostic logs.

## Decision

Use `X-Correlation-ID` as the HTTP request identifier.

- Reuse canonical UUID values from clients.
- Generate a server UUID otherwise.
- Return the effective ID in the response header.
- Store it in SLF4J MDC as `correlationId`.
- Clear or restore MDC after the request.
- Execute the filter within Spring Security.

The standard log pattern includes the MDC value.

## Security

- Do not use correlation IDs as credentials.
- Do not trust an ID as proof of event ownership.
- Do not log passwords, credentials or authorization
  headers.
- Keep authorization independent of observability.

## Limitations

- Background tasks do not automatically inherit MDC.
- Cross-service propagation has not been implemented.
- Exceptional servlet dispatches require future review.
