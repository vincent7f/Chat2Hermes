---
name: publish-github-release-remote
description: Builds release artifacts (e.g. Android APK), creates a GitHub Release on the repo remote with attached assets and release notes, then deploys the same packaged files to a user-configured remote server via SSH/SCP or rsync. Use when the user asks to publish a GitHub release, ship an APK release, upload artifacts to a server, or automate release plus remote deployment.
---

# Publish GitHub Release and deploy to remote server

## When to apply

- The user wants a **new GitHub Release** (tag + notes + downloadable assets).
- The user also wants artifacts **copied to a remote machine** (staging server, file host, internal APK mirror).
- Phrases: **发版**、**打 GitHub Release**、**APK 上传服务器**、**publish-github-release-remote**。

## Preconditions

1. **`owner/repo`** from `git remote get-url origin`; working tree clean or intentional release commit.
2. **Version**: agree on tag name (e.g. `v0.4.0`) and whether it already exists on the remote.
3. **Secrets**: `GH_TOKEN` / `gh auth` for GitHub; SSH key or password flow for the server—**never** echo tokens or paste them into commits.

## Package (this repository)

1. From repo root, run a **full** Android build appropriate to the release (often **`.\gradlew.bat :app:assembleRelease`** or **`assembleDebug`** for internal builds—match user intent).
2. Collect **paths to attach**:
   - Default debug path: `app/build/outputs/apk/debug/app-debug.apk`.
   - If `archiveHerdroidDebugApk` (or similar) runs, use the **timestamped** path from Gradle output (e.g. `Herdroid-debug-*.apk`).
   - Add optional **`RELEASE_NOTES.md`** snippet or excerpt from **`Release.md`** for the target version.
3. Fail the workflow if the build fails; do not publish empty or stale APKs.

## GitHub Release

Prefer **GitHub CLI** when authenticated:

```bash
gh release create TAG --repo OWNER/REPO --title "TITLE" --notes-file NOTES.md PATH/TO/app.apk [more.apk ...]
```

- Generate **`NOTES.md`** from the matching **`## \`vX.Y.Z\``** section in **`Release.md`** or a short changelog.
- If `gh` cannot create releases (token scope), use **REST** `POST /repos/{owner}/{repo}/releases` with `upload_url` for assets—still without logging secrets.

## Remote server deployment

1. **Inputs** (environment or user-provided, not hardcoded in repo):
   - `REMOTE_HOST`, `REMOTE_USER`, `REMOTE_PATH` (e.g. `/var/www/apk/`).
   - Auth: **`ssh` key** already loaded, or **`scp`/`rsync`** with `StrictHostKeyChecking` policy defined by the user.
2. **Copy** the **same** files uploaded to the release (or a subset), e.g.:

   ```bash
   scp -p app/build/outputs/apk/debug/Herdroid-debug-*.apk "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}"
   ```

   Or `rsync -avz …` for multiple files.
3. **Verify**: list remote path or checksum on server if the user cares.
4. **Rollback**: keep previous APK on server with a version suffix if the user follows that convention.

## Order of operations (recommended)

1. Build → verify artifacts exist.
2. Create **git tag** `TAG` at the release commit (if not exists): `git tag -a TAG -m "…"` then `git push origin TAG`.
3. **`gh release create`** with notes + assets **or** create release then upload assets via API.
4. **Deploy** to remote with `scp`/`rsync`.
5. Summarize: GitHub Release URL, file names, remote path—no secrets.

## Relation to other skills

- **herdroid-release-apk** / **create-items-in-release**: documentation and **debug** APK verification during development; this skill is for **tagged releases** and **server upload**.
- **assemble-apk-after-changes.mdc**: still applies before claiming a successful build.

## Safety

- Do not **force-push** tags without explicit user approval.
- **Draft** release first if the user is unsure (`gh release create --draft`).
- Large uploads: respect GitHub **asset size limits** and server **disk quota**.

## Checklist (agent)

- [ ] Built artifacts exist and paths are correct
- [ ] Release notes sourced from `Release.md` or agreed text
- [ ] GitHub Release created with assets; link returned
- [ ] Remote copy completed (or documented failure)
- [ ] No secrets in logs or git history
