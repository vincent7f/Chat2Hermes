---
name: create-github-issue
description: Turns each line of user-supplied text into one GitHub issue on the repo remote, normalizing bodies and grouping issue titles by similar line prefixes (brackets, keywords, list markers). Use when the user pastes a multi-line backlog or task list and wants it filed as separate issues, or says create issues from lines / normalize lines to GitHub issues.
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

## Submission

1. Resolve repo: `git remote get-url origin` → `owner/repo`.
2. Prefer **GitHub CLI** when available and authenticated:

   ```bash
   gh issue create --repo OWNER/REPO --title "TITLE" --body-file -   # or --body "..."
   ```

   Run **one command per line** (or a short script loop). Respect API rate limits; batch large lists in chunks if needed.

3. If `gh` is missing or not logged in: use **GitHub REST API** `POST /repos/{owner}/{repo}/issues` with `GITHUB_TOKEN` (classic: `repo` scope) or **`gh auth token`**. Never print the token. Do not commit tokens.

4. After creation, summarize **issue numbers and URLs** for the user.

## Safety

- **Dry-run**: If the list is long or destructive, show the planned title/body pairs and confirm once.
- **Duplicates**: If the user might re-run the same text, mention checking for duplicate titles or use labels like `imported-backlog` once per batch.
- **Permissions**: Creating issues requires write access to the repo; failures must surface the HTTP error without leaking secrets.

## Checklist

- [ ] `owner/repo` resolved from `origin`
- [ ] Each line → one issue; titles follow shared prefix rules
- [ ] Body includes verbatim source line
- [ ] `gh issue create` or API used; results reported with links
