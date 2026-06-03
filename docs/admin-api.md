# Admin API

`augur-rule-api` is the Spring shell for rule authoring and validation. Rule
management commands, events, draft state, and command processing live in the
Spring-free `augur-rule-management` module.

The first API surface is intentionally small. It accepts canonical rule JSON,
validates whether it can become a `RuleSetSnapshot`, and exposes an in-memory
rule-set draft workflow. It does not publish, durably persist, cache, or
evaluate rules.

## Validate Rule Set

```http
POST /rule-sets/validate
Content-Type: application/json
Accept: application/json
```

The request body is canonical rule JSON as documented in
[rule-json-format.md](rule-json-format.md).

Successful validation returns `200 OK`.

```json
{
  "valid": true,
  "summary": {
    "version": 1,
    "flagCount": 1
  },
  "error": null
}
```

Invalid draft JSON returns `422 Unprocessable Entity` as a Spring
`ProblemDetail` response through `kopring-bricks` Web MVC error handling.

```json
{
  "type": "about:blank",
  "title": "Rule set validation failed",
  "status": 422,
  "detail": "Rule set validation failed.",
  "instance": "/rule-sets/validate",
  "code": "invalid_rule_set",
  "violations": [
    {
      "code": "serve_type_mismatch",
      "message": "Rule 018ff7c1-9354-7b02-b021-76d2791d6a21 for flag new_checkout serves String but default value is Boolean."
    }
  ]
}
```

## Boundary

- API modules may depend on `augur-rule-core` and `augur-rule-json`.
- API modules may depend on `augur-rule-management` when exposing rule
  management workflows.
- Spring API error handling should use `kopring-bricks` starters rather than
  local global exception plumbing.
- API modules must not depend on `augur-rule-sdk`.
- SDK modules must not depend on this API module.
- Persistence, publishing, approval, and audit APIs will be added outside the
  SDK evaluation path.

## Rule Set Drafts

The draft API is backed by an in-memory `RuleManagementEventStore`. It proves the
Spring shell can consume the Spring-free `augur-rule-management` module before
adding durable storage.

```http
POST /rule-set-drafts
Content-Type: application/json
Accept: application/json
```

```json
{
  "version": 1
}
```

Successful creation returns `201 Created`.

```http
ETag: "1"
```

```json
{
  "eventType": "RuleSetDraftCreated",
  "draft": {
    "draftId": "01972f7b-6d6d-7a2a-9e89-0c389fd26e56",
    "ruleSetVersion": 1,
    "status": "Draft",
    "flagCount": 0,
    "streamVersion": 1
  }
}
```

```http
GET /rule-set-drafts/{draftId}
Accept: application/json
```

Returns the current replayed draft state with an `ETag` generated from the
current draft `streamVersion`, or `404 Not Found` with `code=draft_not_found`.

```http
ETag: "4"
```

```http
POST /rule-set-drafts/{draftId}/flags
Content-Type: application/json
Accept: application/json
If-Match: "1"
```

The request body is a canonical flag JSON object from
[rule-json-format.md](rule-json-format.md). Successful changes return the next
draft `streamVersion` as the response `ETag`.

```http
POST /rule-set-drafts/{draftId}/flags/{flagKey}/rules
Content-Type: application/json
Accept: application/json
If-Match: "2"
```

The request body is a canonical rule JSON object from
[rule-json-format.md](rule-json-format.md). Successful changes return the next
draft `streamVersion` as the response `ETag`.

```http
POST /rule-set-drafts/{draftId}/validate
Accept: application/json
If-Match: "3"
```

Validation records a `RuleSetDraftValidated` event when the replayed draft can
become a `RuleSetSnapshot`. Invalid draft JSON or invalid draft state returns
`422 Unprocessable Entity` through `kopring-bricks` Web MVC error handling.

Draft change endpoints use `kopring-bricks` `concurrency-control-starter`.
Missing `If-Match` returns `428 Precondition Required`, stale `If-Match` returns
`412 Precondition Failed`, and append-time stream conflicts return
`409 Conflict`.

Rule management storage is wired through `augur-rule-event-sourcing`, which
adapts the Augur `RuleManagementEventStore` port to the `kopring-bricks`
`EventSourcingTemplate`. The API module provides a non-durable
`InMemoryBricksEventStore` fallback for local development. Disable that fallback
with `augur.rule-management.event-store.in-memory.enabled=false` when wiring a
durable bricks `EventStore`, such as the PostgreSQL-backed starter store.

## Rule Management Errors

Rule management endpoints return Spring `ProblemDetail` responses through
`kopring-bricks` Web MVC error handling. Augur-owned management errors use
stable snake_case `code` values. Bricks concurrency errors keep the starter's
uppercase code values.

| HTTP status | Code | When |
| --- | --- | --- |
| `400 Bad Request` | `invalid_draft_id` | Draft id path variable is not a UUID. |
| `400 Bad Request` | `invalid_flag_key` | Flag key path variable does not match the domain format. |
| `400 Bad Request` | `invalid_rule_set_version` | Draft creation version is not positive. |
| `404 Not Found` | `draft_not_found` | The requested draft stream does not exist. |
| `404 Not Found` | `flag_not_found` | A command targets a missing flag. |
| `404 Not Found` | `rule_not_found` | A command targets a missing rule. |
| `409 Conflict` | `draft_already_exists` | A create command targets an existing draft. |
| `409 Conflict` | `draft_id_mismatch` | Loaded draft state does not match the command draft id. |
| `409 Conflict` | `draft_is_not_editable` | A command tries to edit a non-draft draft. |
| `409 Conflict` | `draft_is_not_publishable` | A command tries to publish a draft in the wrong state. |
| `409 Conflict` | `draft_is_not_archivable` | A command tries to archive a draft in the wrong state. |
| `409 Conflict` | `flag_already_exists` | A command tries to add an existing flag key. |
| `409 Conflict` | `rule_already_exists` | A command tries to add an existing rule id. |
| `409 Conflict` | `stream_version_conflict` | Append-time stream version check fails. |
| `409 Conflict` | `rule_management_event_replay_failed` | Stored management events cannot be replayed. |
| `422 Unprocessable Entity` | `invalid_json` | Canonical flag or rule JSON cannot be decoded. |
| `422 Unprocessable Entity` | `serve_type_mismatch` | Rule serve value type does not match the flag default value type. |
| `422 Unprocessable Entity` | `invalid_rule_set_draft` | Draft validation fails before snapshot creation. |
| `428 Precondition Required` | `PRECONDITION_REQUIRED` | Required `If-Match` header is missing. |
| `412 Precondition Failed` | `PRECONDITION_FAILED` | `If-Match` does not match the current draft `ETag`. |

## Kopring Bricks Adoption Map

`augur-rule-api` should stay a thin Spring shell around Augur-owned
`augur-rule-management` behavior. Add `kopring-bricks` starters only when the
API behavior uses them.

Already used:

- `webmvc-error-starter`: validation errors are returned as Spring
  `ProblemDetail` responses through `ApiException`.
- `concurrency-control-starter`: use `ETagGenerator` and `IfMatchValidator` for
  versioned draft or published rule-set updates. Do not model ETags in
  `RuleSetSnapshot`; keep them in the HTTP shell.
- `event-sourcing-starter`: use the starter's `EventStore` or
  `EventSourcingTemplate` for append/load infrastructure if drafts are persisted
  as event streams. Keep Augur command names, event classes, replay logic, and
  draft state in Augur.

Use when durable rule management storage is added:

- `vt-jdbc-client-starter`: use this instead of local JDBC plumbing when a
  PostgreSQL-backed bricks starter needs a `JdbcClient`.

Use when operator visibility or downstream publication is added:

- `audit-log-starter`: publish operator-visible rule authoring, validation,
  publish, archive, approval, and rollback audit events.
- `outbox-starter`: durably record rule-change notifications, snapshot
  invalidation events, or downstream sync triggers before external publication.

Do not add for the current validation-only API:

- cache, resilience, HTTP client, logging observation, and tracing starters.
  They are useful for future adapters, but the current API has no outbound I/O,
  cache, or runtime fetch behavior.

If a future Augur adapter needs reusable behavior that is not exposed by a
starter, request the missing capability in `kopring-bricks` instead of adding a
private Spring framework layer here.
