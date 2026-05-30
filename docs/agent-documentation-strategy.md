# Agent Documentation Strategy

Agent-facing documentation and human-facing documentation have different jobs.

## Human-Facing Docs

Human-facing docs include:

- `README.md`
- `README.ko.md`
- `docs/adr/*`

These documents should explain context, motivation, tradeoffs, and current
project direction. They can be narrative and selective.

## Agent-Facing Docs

Agent-facing docs include:

- `AGENTS.md`
- `docs/generated/project-facts.md`
- `docs/prompts/*`

These documents should be factual, explicit, and easy for automation to follow.
They should avoid ambiguity and should not invent intentions beyond the code,
generated facts, or accepted ADRs.

## Rules

- Keep `AGENTS.md` focused on stable working rules.
- Keep `docs/generated/project-facts.md` generated and factual.
- Use README files for human orientation, not exhaustive agent instructions.
- Update `docs/generated/project-facts.md` after changing modules,
  dependencies, declarations, or project principles.
- If human docs and generated facts disagree, generated facts should trigger a
  documentation update rather than being ignored.
- When stable working practices change, update `AGENTS.md` first, then update
  ADRs or README files only when they are useful for human context.
