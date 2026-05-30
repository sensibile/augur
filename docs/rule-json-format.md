# Rule JSON Format

This document describes Augur's canonical JSON rule storage format.

The JSON adapter lives in `augur-rule-json`. It converts JSON into the pure
domain model in `augur-rule-core`; validation is performed before a rule set is
treated as evaluation-ready.

## Rule Set

```json
{
  "version": 1,
  "flags": []
}
```

- `version`: positive integer rule set version.
- `flags`: array of flag definitions. Flag keys must be unique within the file.

Use `RuleJson.decodeRuleSet` for draft/editable JSON. Use
`RuleJson.decodeRuleSetSnapshot` for evaluation-ready snapshots.

## Flag

```json
{
  "key": "new_checkout",
  "enabled": true,
  "defaultValue": false,
  "rules": []
}
```

- `key`: flag key. It must match `[a-z][a-z0-9_\-.]{0,127}`.
- `enabled`: when `false`, evaluation returns `defaultValue` with
  `FlagDisabled`.
- `defaultValue`: fallback value when no rule matches.
- `rules`: ordered list of rules. The first matching rule wins.

All `serve` values inside a flag must have the same value type as
`defaultValue`.

## Rule

```json
{
  "id": "01890f2e-7cc3-7cc3-8c4f-123456789abc",
  "serve": true,
  "condition": {
    "type": "predicate",
    "field": "country",
    "op": "Eq",
    "value": "KR"
  }
}
```

- `id`: UUID v7 rule identifier. Rule ids must be unique across the whole rule
  set, not only within a flag.
- `serve`: value returned when the rule matches.
- `condition`: boolean condition tree.

## Values

Rule values are represented as JSON primitives, arrays, or `null`.

```json
"KR"
19
true
null
["pro", "team"]
```

Supported value types:

- string
- finite number
- boolean
- list
- null

Objects are not supported as rule values.

## Conditions

Augur uses `type` as the condition discriminator.

### All

```json
{
  "type": "all",
  "conditions": []
}
```

Matches when every child condition matches. `conditions` must not be empty in a
rule set snapshot.

### Any

```json
{
  "type": "any",
  "conditions": []
}
```

Matches when at least one child condition matches. `conditions` must not be
empty in a rule set snapshot.

### Not

```json
{
  "type": "not",
  "condition": {
    "type": "predicate",
    "field": "blocked",
    "op": "Eq",
    "value": true
  }
}
```

Matches when the child condition does not match.

### Predicate

```json
{
  "type": "predicate",
  "field": "country",
  "op": "Eq",
  "value": "KR"
}
```

- `field`: attribute key. It must match `[a-zA-Z_][a-zA-Z0-9_\-.]{0,127}`.
- `op`: operator name.
- `value`: expected rule value. It defaults to `null` when omitted.

## Operators

| Operator | Expected value | Evaluation |
| --- | --- | --- |
| `Eq` | string, number, boolean, or null | actual equals expected |
| `NotEq` | string, number, boolean, or null | actual exists and differs from expected |
| `In` | list | expected list contains actual |
| `NotIn` | list | actual exists and expected list does not contain actual |
| `GreaterThan` | number | actual number is greater than expected |
| `GreaterThanOrEqual` | number | actual number is greater than or equal to expected |
| `LessThan` | number | actual number is less than expected |
| `LessThanOrEqual` | number | actual number is less than or equal to expected |
| `Exists` | null | actual attribute exists |
| `Missing` | null | actual attribute is missing |
| `Contains` | string, number, boolean, or null | actual string contains expected string, or actual list contains expected |
| `StartsWith` | string | actual string starts with expected |
| `EndsWith` | string | actual string ends with expected |

## Complete Example

```json
{
  "version": 1,
  "flags": [
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": [
        {
          "id": "01890f2e-7cc3-7cc3-8c4f-123456789abc",
          "serve": true,
          "condition": {
            "type": "all",
            "conditions": [
              {
                "type": "predicate",
                "field": "country",
                "op": "Eq",
                "value": "KR"
              },
              {
                "type": "predicate",
                "field": "plan",
                "op": "In",
                "value": ["pro", "team"]
              }
            ]
          }
        }
      ]
    }
  ]
}
```

This rule returns `true` for `new_checkout` when `country` is `KR` and `plan` is
either `pro` or `team`; otherwise it returns the flag default value `false`.
