---
name: handle-github-issue
description: Fetches GitHub issues from the repo remote, implements each issue’s requirements in code or docs, marks the matching Release.md bullet complete with a git commit on success, or records failure in Release.md and posts the reason as an issue comment on failure. Use when the user asks to work through issues, close issues by implementing them, or batch-process GitHub backlog with Release.md updates.
---

# Handle GitHub issues (implement → Release.md → commit)

## When to apply

- The user wants to **implement** open (or listed) GitHub issues **one by one** and keep **`Release.md`** in sync.
- The user says **handle issues**, **fix issues from GitHub**, **按 issue 实现**, or **跑完 issue 列表**.

## Scope and limits

- **One issue at a time** in a clear order: read → implement → verify → update `Release.md` → commit (success path).
- If an issue is **too large** or **ambiguous**, comment on the issue with questions or split the work; do not mark complete in `Release.md` until truly done.
- Match existing project patterns (Kotlin/Android, `Release.md` checklist style, `[Cursor]` commits).

## Inputs

- **Repo**: `owner/repo` from `git remote get-url origin` unless the user specifies another remote.
- **Which issues**: Default **open issues** (`gh issue list --state open`); the user may narrow by **label**, **milestone**, **issue numbers**, or **search query**.

## Per-issue workflow

### 1. Read the issue

- Fetch body and title: `gh issue view N --repo OWNER/REPO --json title,body,number,state,labels` (or web UI if `gh` unavailable).
- Parse acceptance criteria from the issue body (e.g. **Source line**, **Notes**, checklists).

### 2. Implement

- Change **only** what the issue requires; follow repository conventions.
- Run **relevant** checks (e.g. `./gradlew :app:assembleDebug` for app changes per `.cursor/rules/assemble-apk-after-changes.mdc` when applicable).
- Do **not** claim success without running verification appropriate to the change.

### 3. Success path

1. **`Release.md`**: Find the bullet that tracks this issue (same `#N` / link as in [create-github-issue](../create-github-issue/SKILL.md)).  
   - Set the checkbox to **done**: `- [x] …`  
   - Append **implementation note** in the same style as `v0.3.0`: short Chinese description + **`实现 commit：`** `<short-hash>` (and optional PR/issue refs if useful).
2. **Git**: Stage code + `Release.md`; commit with prefix **`[Cursor]`** and a message that references the issue, e.g. `[Cursor] feat(scope): … (#N)`.
3. Optionally **close** the issue with `gh issue close N --comment "…"` if the user expects closure on completion (confirm if unsure).

### 4. Failure path

1. **`Release.md`**: On the matching bullet, **do not** mark `[x]`. Add a **failure marker** in-line so it is visible in the release notes, e.g.  
   `- [ ] … （未交付：简述原因）`  
   or a single trailing note **`——尝试记录：失败原因摘要`** on that line. Keep the **issue link** intact.
2. **GitHub**: Post a comment on the issue with **why it failed** (error message, blocker, missing permission, scope too large, test failure summary). Use `gh issue comment N --body "..."`.
3. **Git**: If partial work should be kept, commit with **`[Cursor]`** and message **`[Cursor] wip: … (#N)`** or **`[Cursor] chore: document blocked issue … (#N)`** only when the user agrees to land partial/docs changes; otherwise leave working tree explained in the issue comment.

## Ordering

- Process issues in **ascending number** or **user-specified order**.
- If **dependencies** exist (issue B blocks A), implement in dependency order or comment on the blocked issue.

## Tooling

- Prefer **`gh`** (`issue list`, `issue view`, `issue comment`, `issue close`) with repo from `origin`.
- Never print or commit **tokens**. If API is used, use env `GITHUB_TOKEN` / `gh auth token` without echoing values.

## Relation to other skills

- **create-github-issue**: Creates issues and writes **`#N` + URL** on `Release.md` bullets; this skill **consumes** those links to find the right row.
- **herdroid-release-apk**: Broader release + APK verification when **shipping** a version; this skill focuses on **issue-driven** implementation and checklist updates.

## Checklist (agent)

- [ ] `owner/repo` and issue list resolved
- [ ] Each issue: read → implement → verify → then edit `Release.md`
- [ ] Success: `- [x]` + commit hash note; **`[Cursor]`** commit including `Release.md`
- [ ] Failure: `Release.md` shows not-done + failure hint; issue gets **comment** with cause; no false `[x]`
