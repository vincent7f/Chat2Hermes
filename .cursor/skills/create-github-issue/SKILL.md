---
name: create-github-issue
description: Turns each line of user-supplied text into one GitHub issue on the repo remote, normalizing bodies and grouping issue titles by similar line prefixes (brackets, keywords, list markers). Appends each item to the latest Release.md version section and records each new issue number and URL on that bullet for tracking. Use when the user pastes a multi-line backlog, wants issues filed from lines, or wants Release.md kept in sync with backlog items and GitHub issue links.
---

# Create GitHub issues from line-oriented text

## When to apply

- The user provides **multi-line text** and wants **one GitHub issue per non-empty line** (or per meaningful line after skipping blanks / optional comment lines).
- The user asks to **classify titles** by patterns at the **start** of each line (same bracket tag, same keyword, same numbering style, etc.).

## Inputs

- **Text**: Usually pasted in chat; may be bullet lists, numbered lists, or freeform lines.
- **Target repo**: Default **`origin`** from `git remote get-url origin`. Parse `owner/repo` from HTTPS or SSH URLs. If ambiguous, ask once.

## Line processing

1. **Split** on newlines. Strip each line; **drop** fully empty lines.
2. Optionally **skip** lines that are clearly not tasks: lines starting with `#` only as comments (unless the user says to include them).
3. **One issue per retained line** unless the user explicitly asks to merge lines.

## Title classification (按行首类似归类)

Detect a **prefix pattern** at the start of each line and reuse the **same title pattern** for lines that share it:

| Line start pattern | Title pattern (examples) |
|-------------------|-------------------------|
| `[Label]` or `【标签】` | `[Label] <short subject>` — subject = rest of line trimmed, max ~80 chars |
| `feat:` / `fix:` / `docs:` / `chore:` / `refactor:` | `[feat] …` / `[fix] …` — map conventional prefix to bracket form |
| `- ` / `* ` / `• ` | Strip marker; derive title from remainder |
| `1.` / `1)` numbering | Strip number; optional label `[#n]` only if user wants traceability |
| `类别：` / `Category:` before body | Use `类别` as bracket label |

**Shortening**: If the remainder is long, keep a **clear noun phrase** in the title; move the full raw line into the body.

**Consistency**: Lines with the **same** detected prefix class should use the **same** bracket/tag style in titles for that batch.

## Body normalization

Use a small fixed template:

```markdown
## Source line

<verbatim line>

## Notes

- (Optional) bullets if the agent infers acceptance criteria or constraints from context.
```

If the user provided **global context** above the list, repeat one short **Background** section on **every** issue or on the **first** issue only—prefer **first issue + link** if GitHub supports it; otherwise repeat a one-line pointer in each body.

## Release.md sync (this repository)

After normalizing lines (and **in addition to** creating issues), update **`Release.md`** at the repository root so each retained line appears as a **planned change** under the correct release.

### Which version section

1. Default: use the **latest** `## \`vX.Y.Z\`` (or `vX.Y.Z-name`) section that represents the **current** release backlog—typically the **last** such heading **above** `## License` (or above the end of the file if no License block).
2. If the user names a version (e.g. “记入 **v0.5.0**” / “under v0.4.0”), edit that section instead.
3. If no matching section exists, **create** a new `## \`vX.Y.Z\`` block in the same style as `v0.4.0` (short intro line + checklist), placed **above** `## License` and **below** the previous version section.

### What to add

- For **each** retained input line, add **one** checklist item: `- [ ] …`, using the **same** prefix convention as issue titles (e.g. `**\[doc]** …`, `**\[feature]** …`) and **concise Chinese** (or match the existing bullets’ language in that section).
- **Do not** duplicate: if a bullet with the same meaning is already present, skip or merge; prefer matching the repo’s existing `Release.md` tone (see `v0.3.0` / `v0.4.0` examples).
- Order: append **below** existing `- [ ]` / `- [x]` items in that version’s list unless the user asks otherwise.

### Issue ID and link on each bullet (required when creation succeeds)

When an issue is **created successfully**, the matching `Release.md` bullet **must** include **both**:

1. **Issue number** (e.g. `#12`).
2. **Full GitHub issue URL** (HTTPS), as a **markdown link** so readers can open it in one click.

**Recommended format** (pick one style and use it consistently within the same batch):

```markdown
- [ ] **\[feature]** 简短描述。跟踪：[#12](https://github.com/OWNER/REPO/issues/12)
```

or suffix form:

```markdown
- [ ] **\[feature]** 简短描述（[#12](https://github.com/OWNER/REPO/issues/12)）
```

- Parse **`owner/repo`** from `git remote get-url origin` to build `https://github.com/OWNER/REPO/issues/N` if the CLI only returns `#N`.
- Prefer capturing **`number`** and **`url`** from `gh issue create --json number,url` (or the API response) so the link is exact.
- **After** all issues are created, edit **`Release.md`** in one pass so every new bullet lines up with its issue **ID + link** (avoid mismatches between rows and issue numbers).

### When issue creation fails

If `gh` / API **cannot** create an issue, still add the `- [ ]` bullet **without** a link and add a one-line note for the user (e.g. token scope). Do **not** invent fake IDs or URLs.

### Git

- Stage **`Release.md`** with the same batch as other edits; commit message prefix **`[Cursor]`** per repository rules (e.g. `[Cursor] docs(release): add v0.x.y backlog items`).

### Relation to other skills

- **`herdroid-release-apk`** covers marking items **done** (`- [x]`) and commit refs when shipping; this skill covers **adding** new planned items from line-oriented input.

## Submission

1. Resolve repo: `git remote get-url origin` → `owner/repo`.
2. Prefer **GitHub CLI** when available and authenticated:

   ```bash
   gh issue create --repo OWNER/REPO --title "TITLE" --body-file -   # or --body "..."
   ```

   Run **one command per line** (or a short script loop). Respect API rate limits; batch large lists in chunks if needed.

3. If `gh` is missing or not logged in: use **GitHub REST API** `POST /repos/{owner}/{repo}/issues` with `GITHUB_TOKEN` (classic: `repo` scope) or **`gh auth token`**. Never print the token. Do not commit tokens.

4. After creation, summarize **issue numbers and URLs** for the user (same values as in **`Release.md`**).
5. Confirm **`Release.md`** was updated under the target version: each new bullet that has a corresponding issue includes **`#N`** and a **markdown link** to `https://github.com/OWNER/REPO/issues/N` (or explain skipped / failed rows).

## Safety

- **Dry-run**: If the list is long or destructive, show the planned title/body pairs and confirm once.
- **Duplicates**: If the user might re-run the same text, mention checking for duplicate titles or use labels like `imported-backlog` once per batch.
- **Permissions**: Creating issues requires write access to the repo; failures must surface the HTTP error without leaking secrets.

## Checklist

- [ ] `owner/repo` resolved from `origin`
- [ ] Each line → one issue; titles follow shared prefix rules
- [ ] Body includes verbatim source line
- [ ] `gh issue create` or API used; results reported with links
- [ ] `Release.md` latest (or named) version section updated with one `- [ ]` bullet per new line; no useless duplicates; **each created issue** reflected as **#N + markdown URL** on the matching bullet
