# 0002 Admin API And Snapshot Boundaries

## Status

Accepted

## Context

Augur now has these rule-related modules:

- `augur-rule-core`: pure domain models, validation, and evaluation.
- `augur-rule-json`: canonical JSON adapter for rule storage/transport.
- `augur-rule-management`: Spring-free rule authoring commands, events, draft
  state, and command processing.
- `augur-rule-sdk`: Kotlin consumer facade for evaluating rule set snapshots.

The next major concern is how rules are created, edited, validated, stored,
published, and consumed. These responsibilities must be separated before adding
Spring, persistence, or admin APIs.

## Decision

Rule authoring and rule evaluation are separate workflows.

Admin/API modules will own rule authoring:

- create and edit draft rule sets
- validate draft rule sets through `RuleSetValidator`
- decode and encode canonical JSON through `augur-rule-json`
- persist drafts and published snapshot records
- manage versions, approvals, audit trails, and publishing
- expose HTTP or UI-facing workflows

SDK modules will own rule consumption:

- accept `RuleSetSnapshot`
- build evaluation requests from caller inputs
- evaluate flags through `AugurEvaluator` or `RuleEngine`
- expose typed evaluation results and decision metadata
- stay unaware of JSON, HTTP, storage, admin workflows, and cache refresh

`RuleSetSnapshot` represents a validated rule set that is safe to evaluate in
memory. It does not represent how the snapshot was loaded, when it was fetched,
who published it, or where it is stored.

## Drafts And Snapshots

Draft rule sets are editable and may be invalid.

```text
draft JSON/domain rule set
  -> validate
  -> RuleSetSnapshot
  -> publish/store/distribute
  -> SDK evaluation
```

The admin/API side may store both invalid drafts and valid published snapshot
records. Only validated `RuleSetSnapshot` values cross into evaluation paths.

## Versioning

`RuleSetVersion` remains part of the core rule set. Admin/API modules decide how
versions are allocated and whether they are monotonic per environment, project,
or global namespace.

The evaluator treats versions as metadata for decisions and traces. It does not
allocate or compare versions.

## Time And Loading Metadata

Load time, publish time, cache age, ETag, signatures, environment names, and
source locations are not part of `RuleSetSnapshot` in core.

These values are shell concerns. They may be modeled later in API, storage, or
client/cache modules.

## JSON Boundary

Canonical JSON remains the storage/transport format for rules.

`augur-rule-json` can be used by admin/API and storage boundaries. The SDK
production module must not depend on `augur-rule-json`; if a future client needs
JSON loading, add a separate adapter module rather than coupling JSON into the
core SDK.

## Module Direction

Current and expected future modules:

- `augur-rule-management`: Spring-free rule management domain behavior.
- `augur-rule-api`: Spring/API shell for authoring, validation, publishing, and
  administration.
- `augur-rule-storage-*`: persistence adapters if storage concerns become
  large enough to split.
- Optional client/cache adapter modules if runtime snapshot fetching becomes a
  first-class SDK concern.

These modules must depend inward toward `augur-rule-core`, not the other way
around.

## Consequences

SDK users cannot create or mutate rules through the SDK. This keeps evaluation
clients simpler and avoids mixing authorization, audit, approval, and
persistence concerns into runtime evaluation.

Admin/API work can evolve independently from SDK evaluation as long as it
publishes valid `RuleSetSnapshot` data.

Spring implementation in admin/API modules should reuse `kopring-bricks`
starters for reusable infrastructure concerns such as API errors,
concurrency-control headers, audit logs, outbox publication, and event-store
infrastructure. Augur should keep rule-specific domain behavior local and avoid
duplicating generic Spring plumbing.

The project should keep enforcing these boundaries with
`mise run architecture:check`. If shell checks become too limited, introduce
ArchUnit or a similar architecture test suite.

## Non-Goals

- Do not add rule authoring builders to the SDK unless the project explicitly
  changes the SDK's role.
- Do not make JSON loading a production dependency of `augur-rule-sdk`.
- Do not put publish time, load time, cache freshness, signatures, approval
  state, or audit metadata into `RuleSetSnapshot` in core.
