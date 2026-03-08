# ADR 0002: Standardized Error Contract and Request Correlation

## Status
Accepted

## Context
Clients need stable machine-readable errors and traceability across logs and API responses.

## Decision
- Return a unified error payload with fields: `code`, `message`, `path`, `timestamp`, `requestId`.
- Use stable `ApiErrorCode` values for client logic.
- Propagate `X-Request-Id` (or generate one) and include it in both response headers and error payload.

## Consequences
- Better operational debugging and supportability.
- Backward compatibility expectations for future error schema changes.
