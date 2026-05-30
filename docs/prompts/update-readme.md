# README Update Prompt

Use `docs/generated/project-facts.md` as the factual source of truth.

When updating README content:

- Do not invent modules, dependencies, APIs, or guarantees not present in generated facts.
- Keep the distinction between functional core and imperative shell clear.
- Mention that UUID generation uses UUID v7 when discussing identifiers.
- Mention that tests should prefer real domain objects, fakes, stubs, and Testcontainers over mocks.
- Mention Kover coverage as a way to spot missing tests, not as a target metric.
- Mention Tidy First and typed/function-composed core logic only when explaining development principles.
- Keep JSON AST as the canonical future storage direction unless the generated facts change.
- Keep `README.md` and `README.ko.md` aligned in meaning, but write them naturally for each language.
- Do not put exhaustive agent instructions in README files; use `AGENTS.md` for that.
