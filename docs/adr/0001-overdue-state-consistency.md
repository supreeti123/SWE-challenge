# ADR 0001: Overdue State Consistency Model

## Status
Accepted

## Context
Todo items must automatically become `past due` when `dueAt` has passed, and `past due` items cannot be modified.

## Decision
Use a hybrid model:
- Scheduled batch updater transitions overdue `NOT_DONE` items to `PAST_DUE`.
- API request path performs synchronization and mutation guards to close scheduler timing gaps.

## Consequences
- Stronger read/write consistency than scheduler-only approaches.
- Slightly higher write/read path overhead due to periodic synchronization step.
- Simpler than event-driven or DB-triggered alternatives for this service scope.
