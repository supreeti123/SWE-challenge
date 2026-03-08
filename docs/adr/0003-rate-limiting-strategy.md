# ADR 0003: Write Endpoint Rate Limiting Strategy

## Status
Accepted

## Context
The API should resist burst abuse and protect backend resources.

## Decision
Apply fixed-window, per-IP in-memory rate limiting to todo write operations (`POST`/`PATCH`).

## Consequences
- Simple, low-latency protection with no external dependency.
- Not globally shared across multiple service instances.
- Migration path: replace in-memory counter with distributed limiter (e.g., Redis) when horizontal scale is introduced.
