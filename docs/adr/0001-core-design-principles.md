# 0001 Core Design Principles

## Status

Accepted

## Context

Augur starts as a feature flag and rule branching engine. The project needs a
small, predictable rule core before adding Spring, persistence, or SDK shells.

## Decision

- Keep rule evaluation in a functional core.
- Keep I/O, caching, HTTP, persistence, clocks, random values, and identifier generation in shells or ports.
- Prefer value objects at domain boundaries.
- Use UUID v7 for generated rule identifiers.
- Use a lightweight `Outcome<E, A>` style result instead of Kotlin's default `Result`.
- Store rules in a canonical JSON AST format once serialization is added.
- Treat human-friendly DSL files as compiler inputs to canonical JSON, not as the engine's source of truth.
- Avoid mocking frameworks by default. Use real domain objects, fakes, stubs, and Testcontainers for infrastructure boundaries.
- Use Kover for Kotlin/JVM coverage as an observation tool, not as a target metric.
- Apply Tidy First: keep behavior-preserving structure changes small and separate from feature changes.
- Prefer function composition and typed strategies over large conditional branches in important rule logic.
- Let ktlint own formatting. Detekt should focus on structural issues and avoid duplicate formatting checks.

## Consequences

The core should remain easy to test without mocks. If tests become hard to
write, production design should be questioned before adding a mocking framework.
Adapter modules can evolve independently because the evaluator only depends on
domain inputs and returns deterministic decisions.

Important rule logic should avoid accumulating broad conditional trees. When an
operator or branch must select behavior, keep that selection explicit and push
the selected behavior into small typed functions or strategies.
