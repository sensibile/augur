# Admin API

`augur-rule-api` is the Spring shell for rule authoring and validation.

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
- Spring API error handling should use `kopring-bricks` starters rather than
  local global exception plumbing.
- API modules must not depend on `augur-rule-sdk`.
- SDK modules must not depend on this API module.
- Persistence, publishing, approval, and audit APIs will be added outside the
  SDK evaluation path.
