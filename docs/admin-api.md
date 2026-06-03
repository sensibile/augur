# Admin API

`augur-rule-api` is the Spring shell for rule authoring and validation. Rule
management commands, events, draft state, and command processing live in the
Spring-free `augur-rule-management` module.

The first API surface is intentionally small. It accepts canonical rule JSON and
validates whether it can become a `RuleSetSnapshot`. It does not publish,
persist, cache, or evaluate rules.

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

## Kopring Bricks Adoption Map

`augur-rule-api` should stay a thin Spring shell around Augur-owned
`augur-rule-management` behavior. Add `kopring-bricks` starters only when the
API behavior uses them.

Already used:

- `webmvc-error-starter`: validation errors are returned as Spring
  `ProblemDetail` responses through `ApiException`.

Use when the full rule management HTTP API is added:

- `concurrency-control-starter`: use `ETagGenerator` and `IfMatchValidator` for
  versioned draft or published rule-set updates. Do not model ETags in
  `RuleSetSnapshot`; keep them in the HTTP shell.

Use when durable rule management storage is added:

- `event-sourcing-starter`: use the starter's `EventStore` or
  `EventSourcingTemplate` for append/load infrastructure if drafts are persisted
  as event streams. Keep Augur command names, event classes, replay logic, and
  draft state in Augur.
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
