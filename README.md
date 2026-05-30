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

## Minimal Usage

```kotlin
val decoded = RuleJson.decodeValidRuleSet(json)

val decision = decoded.flatMap { ruleSet ->
    RuleEngine.evaluate(
        ruleSet = ruleSet,
        request = request,
    )
}
```

Typed evaluation APIs are available when the expected flag type is known:

```kotlin
val enabled = decoded.flatMap { ruleSet ->
    RuleEngine.evaluateBoolean(
        ruleSet = ruleSet,
        request = request,
    )
}
```

Use `decodeRuleSet` when working with draft or editable rules. Use
`decodeValidRuleSet` for evaluation-ready snapshots.
