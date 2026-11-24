# AEGIS — Logging Baseline

Roadmap: AEGIS-033.
Status: partial.

## Implemented

- Spring Boot logging.
- HTTP X-Correlation-ID response header.
- SLF4J MDC key: correlationId.
- Log pattern includes correlation ID.

## Pending

- Structured JSON logging.
- Production log retention and rotation.
- Async correlation propagation.
- Centralized logging.

Never log passwords, tokens or Authorization headers.
