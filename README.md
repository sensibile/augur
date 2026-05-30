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

## Modules

- `augur-rule-core`: pure rule domain, validation, and evaluation
- `augur-rule-json`: JSON storage format adapter
- `augur-rule-sdk`: Kotlin-friendly request builders and SDK conveniences

## End-To-End Usage

```kotlin
val ruleSet =
    when (val result = RuleJson.decodeValidRuleSet(json)) {
        is Outcome.Err -> error("Invalid rule set: ${result.error}")
        is Outcome.Ok -> result.value
    }

val request =
    when (
        val result =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                string("country", "KR")
                number("age", 19.0)
            }
    ) {
        is Outcome.Err -> error("Invalid request: ${result.error}")
        is Outcome.Ok -> result.value
    }

val enabled =
    RuleEngine.evaluateBoolean(
        ruleSet = ruleSet,
        request = request,
    )
```

Use `RuleEngine.evaluate` when a generic `RuleValue` decision is preferred.
Use `decodeRuleSet` when working with draft or editable rules. Use
`decodeValidRuleSet` for evaluation-ready snapshots.
