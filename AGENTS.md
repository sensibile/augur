# AGENTS.md

This file defines the working rules for agents contributing to Augur.

## Project Shape

Augur is a Kotlin feature flag and rule branching engine.

The current priority is the Spring-free rule core. Keep the core small,
deterministic, and easy to test before adding API, persistence, SDK, or Spring
shells.

## Build Baseline

- Gradle: `9.4.1`
- Kotlin: `2.3.20`
- Java: `temurin-25`
- Spring Boot version catalog entry: `4.0.6`, but do not add Spring to the core.
- Use `mise` tasks for local workflow.

Common commands:

```sh
./gradlew test
./gradlew koverHtmlReport
mise run lint
mise run architecture:check
mise run coverage
mise run coverage:check
mise run docs:check
mise run docs:facts
```

## Architecture Rules

- Use functional core, imperative shell.
- Keep rule evaluation side-effect free.
- Do not put HTTP, DB, cache refresh, logging, metrics, clocks, random values, or
  identifier generation inside the evaluator.
- Pass all required inputs into pure functions.
- Put side effects behind ports or in outer modules.
- Prefer Gradle module boundaries over package-only boundaries as the project
  grows.
- Run `mise run architecture:check` after changing modules or dependencies.
- Add ArchUnit checks later if module-boundary rules outgrow shell checks.
- Prefer function composition and domain types over large conditional branches
  in important rule logic.
- When branching is unavoidable, isolate it at selection boundaries and keep the
  selected behavior small and typed.

## Tidy First

Follow Kent Beck's Tidy First approach.

- Keep behavior-preserving tidies separate from feature changes whenever
  practical.
- Tidy before feature work when local duplication, naming, or structure makes
  the next change harder to reason about.
- Keep tidies small. If a tidy expands beyond the immediate area, stop and make
  it a separate task.
- Do not introduce abstractions speculatively. Extract only after real
  duplication or complexity appears.
- Run tests and relevant lint after tidies.
- If a tidy changes behavior, it is no longer a tidy; treat it as feature or bug
  work and test it explicitly.

## Review Loop

- After each implementation pass, review the changed code before handing off.
- Prioritize bugs, boundary regressions, API compatibility, missing tests, and
  architecture rule violations.
- Apply necessary fixes found during review in the same task when they are
  clearly scoped.
- Re-run the relevant tests and checks after review fixes.
- Call out any remaining risks that are intentionally left unfixed.

## Core Module Rules

`augur-rule-core` must stay infrastructure-free.

Do not add these to `augur-rule-core`:

- Spring
- Jackson or JSON serialization
- HTTP clients
- database clients
- cache libraries
- logging frameworks
- metrics/tracing libraries
- Testcontainers
- mocking frameworks

Serialization belongs in a future adapter module such as `augur-rule-json`.
SDK fetch/cache behavior belongs outside the core.

## Module Boundary Rules

- `augur-rule-core` is the functional core and must not depend on other Augur
  modules.
- `augur-rule-json` is a serialization adapter. Its production code may depend
  on `augur-rule-core`, but not on `augur-rule-sdk`.
- `augur-rule-sdk` is a rule evaluation consumer facade. Its production code may
  depend on `augur-rule-core`, but not on `augur-rule-json`, HTTP, storage, or
  admin/API modules.
- Rule creation, editing, persistence, approval, and audit workflows belong in
  admin/API modules, not in the SDK.

## Domain Modeling

- Use value objects aggressively at domain boundaries.
- Avoid passing raw `String`, `Long`, or `UUID` values when a domain type makes
  the meaning clear.
- Prefer sealed interfaces and data classes for closed domain models.
- Prefer typed strategies, small pure functions, and function composition over
  broad `when` or `if` trees in core logic.
- Use a lightweight `Outcome<E, A>` style result type instead of Kotlin's default
  `Result`.
- Treat expected non-matches as normal decisions, not errors.
- Reserve errors for invalid rulesets, invalid inputs, parsing failures, and
  unsupported engine states.

## UUID Rules

- Generated identifiers must use UUID v7.
- Prefer Kotlin's `kotlin.uuid.Uuid` API.
- Keep any `ExperimentalUuidApi` opt-in narrow.
- Identifier generation is a side effect. Keep it outside evaluation paths or
  behind a small port such as `RuleIdGenerator`.

## Rule DSL Direction

- Canonical rule storage should be JSON AST.
- The evaluator should depend on Kotlin domain models, not raw JSON.
- Human-friendly file DSLs may be added later as compiler inputs to canonical
  JSON AST.
- Do not make a human DSL the engine's source of truth.

## Testing Rules

- Do not add `mockk` or other mocking frameworks by default.
- Mocks are a last resort after fake, stub, fixture, and design alternatives are
  exhausted.
- Core tests should use real domain objects and deterministic inputs.
- If core tests are hard to write without mocks, question the production design
  first.
- Use fake or stub implementations for ports such as clocks, ID generators, or
  external gateways.
- Infrastructure tests should use real boundaries where practical, especially
  Testcontainers for databases.
- Do not use Testcontainers in the core module.

## Lint And Formatting

Do not add Gradle ktlint or detekt plugins unless explicitly requested.

This project follows the `kopring-bricks` style:

- `ktlint` runs as a CLI.
- `detekt` runs as a CLI through `mise`.
- `mise run lint` checks Kotlin style and detekt rules.
- `mise run format:ktlint` formats changed Kotlin files only.
- Formatting ownership belongs to ktlint. Disable or relax detekt rules that
  duplicate formatting concerns when they conflict with ktlint output.

## Coverage

Use Kover for Kotlin/JVM test coverage.

- Treat coverage as an observation tool for finding missing tests, not as a
  target metric.
- Do not optimize implementation or tests to satisfy coverage percentages.
- Do not add coverage thresholds unless explicitly requested.
- Use `mise run coverage` for HTML reports.
- Use `mise run coverage:check` for a log summary.

## Documentation

Generated facts are kept in:

```text
docs/generated/project-facts.md
```

Update generated facts after changing modules, dependencies, source
declarations, or project principles:

```sh
mise run docs:facts
mise run docs:check
```

Use `docs/generated/project-facts.md` as factual source material for README and
human-authored docs. Do not invent modules, APIs, dependencies, or guarantees
not supported by code or generated facts.

Architecture decisions live under `docs/adr`.

## Git Hygiene

- Do not revert user changes unless explicitly asked.
- Keep edits scoped to the requested work.
- Avoid unrelated refactors.
- Run tests and relevant `mise` checks before handing off.
