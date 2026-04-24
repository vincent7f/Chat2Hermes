---
name: dev-apk-release-workflow
description: 通用发布开发流程：按指定版本号与计划文档逐项实现，每项完成即自动提交并回写 Release.md，全部完成后统一构建 APK 并汇报完整文件名与路径。
---

# Dev APK Release Workflow

## 适用场景

- 用户要求“按某个版本计划逐项开发并交付 APK”。
- 用户要求每个修改项自动提交 git。
- 用户要求边做边更新 `Release.md` 对应版本勾选状态。

## 输入参数

- `version`：目标版本号（例如 `v0.5.0`）。
- `plan_doc`：计划文档路径（例如 `docs/v0.5.0-plan.md`）。
- `release_doc`：发布清单路径（默认 `Release.md`）。

## 执行规则

1. 先读取 `plan_doc` 与 `release_doc`，确认目标版本小节为 `## \`<version>\``。
2. 每完成一个独立子项，必须同时完成：
   - 代码/文档实现；
   - 在 `release_doc` 的 `<version>` 小节将对应条目标记为 `[x]`；
   - 在条目后补充实现 commit（短哈希）；
   - 立即执行 `git add -A` 与 `git commit`（提交信息以 `[Cursor]` 开头）。
3. 未完成全部条目前，不执行最终 APK 构建。
4. 当 `<version>` 小节的功能条目全部为 `[x]` 后，执行：
   - `.\gradlew.bat assembleDebug --no-daemon`
   - 明确汇报 APK 全名与完整路径
   - 将“构建完成”条目标记为 `[x]` 并提交一次 git。

## 回写规范

- 回写目标仅限 `release_doc` 的 `## \`<version>\`` 小节。
- 每个完成项推荐格式：
  - `- [x] <条目名>。实现 commit：\`<short-hash>\``
- 若版本小节不存在，先创建版本小节与待办模板，再开始实施。

## 失败处理

- 若子项存在阻塞，不勾选 `[x]`，并记录阻塞原因与建议下一步。
- 禁止在未实现时提前勾选或伪造 commit 信息。
