# ADR 0004: Read Caching Strategy for Todo Lookup

## Status
Accepted

## Context
Frequent read-by-id calls can create unnecessary database load.

## Decision
- Use local Caffeine cache for `GET /api/todos/{id}`.
- Cache only stable item states to reduce stale-read risk.
- Evict or refresh cache entries on state-changing mutations.

## Consequences
- Improved read latency and lower database pressure for hot keys.
- Per-instance cache scope; no cross-instance coherence guarantees.
- Requires explicit invalidation discipline on writes.
