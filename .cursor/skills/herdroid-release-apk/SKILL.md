---
name: herdroid-release-apk
description: Updates Herdroid Release.md (especially the v0.3.0 checklist) to record shipped features and commit references, then runs a full debug APK build. Use when finishing features in this repo, when the user asks to sync Release notes with code changes, or when combining release documentation with assembleDebug verification.
---

# Herdroid：Release.md 与 Debug APK

## When to apply

- 在本仓库完成一项或一批**用户可见功能**（聊天 UI、TTS、设置等）并准备收尾时。
- 用户明确要求**更新 Release**、**写进 Release**、或与 **APK / assembleDebug** 一起交付时。

## Release.md

1. 打开**仓库根目录**下的 `Release.md`。
2. 在 **`## \`v0.3.0\``**（或当前版本小节）下：
   - 新增或改写一条 **`- [x]`**，用**简体中文**写清：功能点、关键类/资源、**实现 commit**（短 hash 或 `git log` 可检索的 message 片段）。
   - 与现有条目风格一致：一句说明 + 括号内技术细节；初版与修订可分写或并写。
3. 若仅为**小修复**且不值得单独成条，可在最近相关条目中补一句「修订：…」。
4. **不要**在 Release 里写自指错误的短 hash（若 amend 会变动）；可写 **message 关键字**（如 `feat(chat): …`）指向提交。
5. 将 `Release.md` 与代码**同一批或紧随其后的提交**纳入 Git；提交信息前缀 **`[Cursor]`**（与仓库规则一致）。

## Debug APK

1. 在仓库根目录执行完整 debug 构建（勿只跑 `compileDebugKotlin`）：

   ```bash
   .\gradlew.bat :app:assembleDebug --no-daemon
   ```

   （PowerShell 可用 `Set-Location` 到项目根再执行。）

2. 成功产物通常在 `app/build/outputs/apk/debug/app-debug.apk`；若 `assembleDebug` 触发了 `archiveHerdroidDebugApk`，以 **Gradle 日志**中的归档路径为准。
3. 构建失败则修复或说明原因，**勿**未验证即声称「已可安装」。

## 与现有规则的关系

- 仓库已有 `.cursor/rules/assemble-apk-after-changes.mdc`：改应用代码后应完整构建；本 skill **额外强调**有功能交付时**同步 Release.md**。

## Checklist（可复制）

```
- [ ] Release.md 目标版本小节已更新（含 [x] 与实现说明）
- [ ] Git 已提交（含 Release 时一并提交）
- [ ] 已执行 assembleDebug 且日志成功
```
