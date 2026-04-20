---
name: better-code
description: Reviews a codebase and directly implements three actions: remove unused code, refactor overly complex logic, and fix confirmed logic bugs. Use when the user asks to "review codes to implement", clean dead code, reduce complexity, or repair behavioral bugs in one pass.
---

# Review, Clean, Refactor, Fix

## When to apply

- The user asks for a combined pass such as:
  - review code and implement improvements
  - remove unused code
  - refactor complicated code
  - fix logic bugs
- The user expects direct implementation instead of review-only feedback.

## Default execution mode

- Default to **implement-first**:
  1. Inspect code and identify concrete targets.
  2. Apply code changes directly.
  3. Run verification commands.
  4. Report results and residual risks.

If the user explicitly asks for planning/review-only, switch to review-only mode.

## Workflow

Use this checklist and keep exactly one item in progress:

```md
Task Progress:
- [ ] 1) Baseline scan
- [ ] 2) Remove unused code
- [ ] 3) Refactor high-complexity paths
- [ ] 4) Fix logic bugs
- [ ] 5) Verify and summarize
```

### 1) Baseline scan

- Map key modules and entry points.
- Search for:
  - unreachable/dead files, functions, imports, or legacy adapters
  - long functions, deeply nested branches, duplicated logic
  - behavior inconsistencies, race conditions, null/empty edge cases
- Prioritize findings by runtime impact and regression risk.

### 2) Remove unused code

- Remove only code that is confirmed unused by references and behavior.
- Safe candidates:
  - unreferenced files/classes/functions
  - obsolete helper methods replaced by current flow
  - stale comments/docs that contradict current architecture
- Avoid deleting placeholders if clearly intended for near-term use unless user requested aggressive cleanup.

### 3) Refactor high-complexity paths

- Refactor without changing behavior:
  - extract private helpers
  - flatten nested conditionals
  - consolidate repeated list/map/update logic
  - split oversized UI or service files into focused units
- Keep public contracts and data formats stable unless bugfix requires change.

### 4) Fix logic bugs

- Fix only reproducible or clearly provable logic issues.
- Prefer minimal, targeted changes with explicit guards for edge cases.
- Typical bug categories:
  - async ordering/race problems
  - incorrect state transitions
  - incorrect fallback/default paths
  - broken error handling branches

### 5) Verify and summarize

- Run project-appropriate verification (build/tests/lints).
- Confirm no newly introduced diagnostics in edited files.
- Summarize:
  - what was removed
  - what was refactored
  - what bug was fixed and why
  - what was validated and what remains unverified

## Output format

When reporting completion, use:

1. **Removed unused code**: file-level list
2. **Refactors**: simplified paths and rationale
3. **Logic bug fixes**: bug cause and fix behavior
4. **Verification**: commands and key results
5. **Residual risk**: only if anything remains

## Guardrails

- Do not claim success without running verification.
- Do not introduce broad architectural rewrites unless requested.
- Do not remove potentially shared code without checking references.
- Keep changes scoped to the three requested goals.
