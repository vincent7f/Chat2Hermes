---
name: create-items-in-release
description: Implements each unchecked item under the latest (or named) version section in repo-root Release.md, marks it complete with an implementation note, and creates a git commit per item or per coherent batch. Use when the user asks to ship a Release checklist, implement all v0.x.y bullets, convert Release backlog to code, or says create-items-in-release / 按 Release 实现清单.
---

# Create items from Release.md (implement → mark → commit)

## When to apply

- The user points to **`Release.md`** (usually **`## \`vX.Y.Z\``**) and wants **every `- [ ]` item** turned into working code/docs, then **checked off** in the same file.
- Phrases: **按 Release 实现**、**v0.x.y 清单做完**、**逐项交付 Release**、**/create-items-in-release**。

## Scope

- **One version section at a time** (default: **last** `## \`v…\`` block **above** `## License`).
- Respect **existing architecture** (Kotlin/Android, DataStore, `[Cursor]` commits); run **`.\gradlew.bat :app:assembleDebug`** (or the project’s full debug build) after app-affecting changes per `.cursor/rules/assemble-apk-after-changes.mdc`.

## Workflow

1. **Read** `Release.md` and list all **unchecked** `- [ ]` lines in the target version.
2. **Order**: top to bottom unless dependencies require reordering; note blockers in commit message or a short comment if an item must be deferred.
3. **Per item (or small related batch)**:
   - Implement the behavior (code + strings + docs as needed).
   - Change the line to **`- [x]`** and add a short **实现说明**：prefer referencing the **commit message** pattern `[Cursor] …`（例如「与 `[Cursor] feat(scope): …` 同批提交」）so hash drift is avoided; optional short hash if stable.
4. **Git**: `git add` relevant files + `Release.md`; **`git commit -m`** with prefix **`[Cursor]`** and a message that matches the work (e.g. `[Cursor] feat(chat): … (#v0.4.0)`).

## Release.md safety (especially on Windows)

- Prefer **editing `Release.md` in UTF-8**; avoid PowerShell `Set-Content` without encoding (can mangle Chinese). If the file corrupts, restore from `git` and re-apply, or rewrite with a **Python** `Path.write_text(..., encoding="utf-8")` one-liner.
- After edits, optionally verify: `assert "计划中" in text` or similar on the decoded UTF-8 string.

## Relation to other skills

- **herdroid-release-apk**: Use when **closing a version** with APK + final `- [x]` polish; this skill focuses on **implementing** checklist items from the backlog.
- **create-github-issue** / **handle-github-issue**: Optional; use if the user also wants GitHub issues filed or driven from the same bullets.

## Checklist (agent)

- [ ] Target `## \`vX.Y.Z\`` section identified; all `- [ ]` processed or explicitly skipped with reason.
- [ ] Each delivered line is `- [x]` with an implementation note.
- [ ] Commits use **`[Cursor]`**; build verified when the app changed.
- [ ] `Release.md` remains valid UTF-8 Chinese/English.
