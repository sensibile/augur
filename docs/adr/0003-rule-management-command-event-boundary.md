# 0003 Rule Management Command Event Boundary

## Status

Accepted

## Context

Rule management will eventually need more than CRUD:

- draft lifecycle
- validation
- publishing
- rollback
- approvals
- audit trails
- cache invalidation or snapshot distribution

These concerns apply whether the admin side is implemented with state-based
storage, an outbox, or event sourcing. The project needs a stable language for
user intent and domain facts before adding persistence or a larger Admin API.

## Decision

Rule management will model commands and domain events in the Spring-free
`augur-rule-management` boundary.

Commands represent user or system intent:

- `CreateRuleSetDraft`
- `AddFlag`
- `EnableFlag`
- `DisableFlag`
- `AddRule`
- `ChangeRuleCondition`
- `ChangeRuleServeValue`
- `RemoveRule`
- `ValidateRuleSetDraft`
- `PublishRuleSet`
- `ArchiveRuleSet`

Domain events represent facts that already happened:

- `RuleSetDraftCreated`
- `FlagAdded`
- `FlagEnabled`
- `FlagDisabled`
- `RuleAdded`
- `RuleConditionChanged`
- `RuleServeValueChanged`
- `RuleRemoved`
- `RuleSetDraftValidated`
- `RuleSetPublished`
- `RuleSetArchived`

The first implementation does not need to use event sourcing. Commands and
events are still useful for naming use cases, audit records, outbox messages,
projections, and future event-sourced storage.

## Ownership

`augur-rule-core` owns rule domain models, validation, snapshots, and
evaluation.

`augur-rule-management` owns:

- command handlers
- domain event records
- draft lifecycle
- publish workflow
- approval workflow

`augur-rule-api` or future admin modules own:

- audit and outbox integration
- persistence and projection concerns
- HTTP-facing rule management workflows

The SDK must not expose rule management commands or events.

## Storage Direction

Both storage styles remain valid:

```text
state-based storage + outbox
  command -> validate -> mutate state -> record domain event -> publish outbox
```

```text
event sourcing
  command -> validate -> append domain event -> project draft/snapshot state
```

Choosing one storage style must not change the core evaluator contract.

## Versioning

`RuleSetVersion` is evaluation metadata in the core rule set. It must not be
treated as the event stream version or database row version.

Admin/API modules should introduce separate value objects when needed, such as:

- `RuleSetDraftId`
- `RuleSetDraftRevision`
- `PublishedRuleSetId`
- `PublishedRuleSetVersion`
- `RuleManagementEventId`
- `RuleManagementStreamVersion`

## Snapshot Boundary

`RuleSetSnapshot` is an execution artifact. It is produced after validation and
used by evaluation paths.

It must not contain command metadata, event metadata, audit state, approval
state, storage offsets, publish timestamps, cache freshness, or stream versions.

## API Direction

Admin APIs should prefer intention-revealing operations over generic CRUD when
the workflow has domain meaning.

Examples:

```text
POST /rule-set-drafts
POST /rule-set-drafts/{draftId}/flags
POST /rule-set-drafts/{draftId}/rules
POST /rule-set-drafts/{draftId}/validate
POST /rule-set-drafts/{draftId}/publish
```

The current `POST /rule-sets/validate` endpoint remains a thin validation
surface. It is not the full rule management API.

## Consequences

The project can begin with simple state-based persistence while still retaining
clear command/event names for audit and integration.

If event sourcing becomes necessary later, the event names and aggregate
boundaries can be reused instead of rediscovered from CRUD handlers.

When the Spring admin/API shell persists, audits, publishes, or exposes these
events over HTTP, it should use existing `kopring-bricks` starters around the
edges: event sourcing for append/load infrastructure, audit log for
operator-visible change records, outbox for durable rule-change publication,
concurrency control for stale update protection, and Web MVC error handling for
ProblemDetail responses.

Authorization systems can follow the same pattern in their own domain while
using Augur rule snapshots as execution artifacts.

## Non-Goals

- Do not put command handlers or events in `augur-rule-core`.
- Do not make `RuleSetSnapshot` an audit, approval, storage, or event-sourcing
  model.
- Do not expose admin command/event APIs from `augur-rule-sdk`.
- Do not force event sourcing before the persistence and audit requirements
  justify it.
