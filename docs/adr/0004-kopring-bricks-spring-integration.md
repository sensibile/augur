# 0004 Kopring Bricks Spring Integration

## Status

Accepted

## Context

Augur is being built near `kopring-bricks`, which already provides reusable
Spring Boot starters for recurring application infrastructure:

- Web MVC error handling and ProblemDetail primitives.
- Optimistic concurrency helpers such as ETag and If-Match validation.
- Event sourcing storage and replay primitives.
- Audit event publishing and JDBC-backed audit storage.
- Transactional outbox storage and publication flow.

Augur should not duplicate this Spring plumbing locally. The rule core and rule
management domain are application-specific, but the Spring edges are expected to
reuse `kopring-bricks` where a starter already exists.

`kopring-bricks` also has a proposed rule-decision starter boundary. Until that
starter exists, Augur owns the rule schema, evaluation engine, snapshots,
management commands, and management domain events.

## Decision

Spring implementation work in Augur will treat `kopring-bricks` starters as the
first integration option.

Use bricks for reusable infrastructure concerns:

- `webmvc-error-starter` for API exception handling and ProblemDetail response
  shape.
- `concurrency-control-starter` for ETag generation, If-Match validation,
  stale-write rejection, and idempotency conflict primitives.
- `event-sourcing-starter` for append/load/replay storage infrastructure when
  rule management uses event-sourced persistence.
- `audit-log-starter` for operator-visible rule authoring, approval, publish,
  archive, and rollback audit events.
- `outbox-starter` for durable publication of rule-change notifications,
  snapshot invalidation messages, or downstream sync triggers.

Keep Augur-owned concerns in Augur:

- Canonical rule JSON and Kotlin rule domain models.
- Functional rule evaluation.
- Rule-management command and event names.
- Draft lifecycle, validation, publish workflow, and approval semantics.
- Snapshot construction and evaluation-facing SDK APIs.

If a Spring concern appears reusable but is not supported by `kopring-bricks`,
open or request a `kopring-bricks` change instead of adding an Augur-only
framework layer.

## Integration Order

For new admin/API work:

1. Read the `kopring-bricks` README and application agent guide.
2. Add only the starter dependency the API behavior uses.
3. Keep app-specific adapters thin and explicit.
4. Add an application-level integration test proving Augur relies on the
   starter behavior.
5. If the starter API is insufficient, record the needed library change before
   adding a temporary Augur workaround.

## Consequences

Augur's core stays deterministic and independent from Spring, while the API
shell avoids reimplementing reusable infrastructure code.

The project may initially keep simple in-memory or fake ports for pure command
tests. Production Spring adapters should move toward bricks-backed
implementations as API, persistence, audit, and publication requirements become
concrete.

## Non-Goals

- Do not move Augur's rule engine into `kopring-bricks`.
- Do not depend on internal `kopring-bricks` autoconfigure modules directly
  unless a starter cannot expose the needed application API.
- Do not add unused starter dependencies only because they may be useful later.
- Do not make `augur-rule-core` depend on `kopring-bricks` or Spring.
