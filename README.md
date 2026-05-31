# Augur

Augur is an experimental Kotlin feature flag and rule branching engine.

The current focus is the Spring-free rule core:

- functional core, imperative shell
- value objects at domain boundaries
- UUID v7 identifiers
- lightweight `Outcome<E, A>` result handling
- tests with real domain objects, fakes, and stubs instead of mocking frameworks
- Kover-based coverage reports for spotting missing tests
- Tidy First changes and typed/function-composed core logic

Generated project facts live at `docs/generated/project-facts.md`.

Korean documentation is available in [README.ko.md](README.ko.md).

The canonical rule storage format is documented in
[docs/rule-json-format.md](docs/rule-json-format.md).
Admin/API and snapshot boundaries are documented in
[docs/adr/0002-admin-api-and-snapshot-boundaries.md](docs/adr/0002-admin-api-and-snapshot-boundaries.md).
Rule management command/event boundaries are documented in
[docs/adr/0003-rule-management-command-event-boundary.md](docs/adr/0003-rule-management-command-event-boundary.md).
The initial Admin API contract is documented in
[docs/admin-api.md](docs/admin-api.md).

## Modules

- `augur-rule-core`: pure rule domain, validation, and evaluation
- `augur-rule-json`: JSON storage format adapter
- `augur-rule-sdk`: Kotlin-friendly request builders and SDK conveniences
- `augur-rule-api`: Spring API shell for rule authoring and validation

## End-To-End Usage

```kotlin
val ruleSet =
    when (val result = RuleJson.decodeRuleSetSnapshot(json)) {
        is Outcome.Err -> error("Invalid rule set snapshot: ${result.error}")
        is Outcome.Ok -> result.value
    }

val evaluator = AugurEvaluator.of(ruleSet)

val enabled =
    evaluator.evaluateBoolean(
        flagKey = "new_checkout",
        targetKey = "user-1",
    ) {
        string("country", "KR")
        number("age", 19.0)
    }
```

Use `RuleEngine.evaluate` directly when a generic `RuleValue` decision is preferred.
Use `decodeRuleSet` when working with draft or editable rules. Use
`decodeRuleSetSnapshot` for evaluation-ready snapshots.
