# 开发指南 — Herdroid

本文档说明本地验证步骤、提交约定及与 Cursor 工作区规则对齐的提交流程。

## 1. 环境

- Android Studio 稳定版（版本随团队统一）。
- JDK 与 Android Gradle Plugin 版本以工程 `gradle` 文件为准。

## 2. 验证（修改后必做）

### 2.1 仅文档变更

- 通读修改段落，确认与 [Ideas.md](Ideas.md)、[PRD.md](PRD.md) 无矛盾。
- 检查 Markdown 内链路径正确；含 Mermaid 的文件在支持预览的编辑器中渲染正常。

### 2.2 含 Android 代码的变更（工程就绪后）

在仓库根目录执行（Windows 可用 `gradlew.bat`）：

```bash
./gradlew lint test assembleDebug
```

Debug 包：`app/build/outputs/apk/debug/app-debug.apk`；构建结束后还会生成 `Herdroid-debug-<yyyyMMdd-HHmmss>.apk`（同目录），并复制到 `D:\BaiduSyncdisk\apk\Herdroid\`（需已配置 `local.properties` 中的 `sdk.dir`）。

- **lint**：静态检查与 Android 专项规则。
- **test**：单元测试。
- **assembleDebug**：确保可编译。

若引入 CI，以 CI 绿灯为合并前置条件；本地至少执行与 CI 等价的最小子集。

### 2.3 完成声明

在对外说明「已完成」前，须完成本节对应验证；关键业务逻辑应有单元测试覆盖（随代码补充）。

## 3. Git 工作流

- **分支**：`main` 为稳定线；功能分支 `feat/`、`fix/` 等可选。
- **提交信息**：建议 [Conventional Commits](https://www.conventionalcommits.org/) 风格；本仓库 Cursor 规则要求消息以 **`[Cursor]`** 前缀开头（见下节自动化）。

## 4. 修改 → 验证 → 确认 → 提交（自动化约定）

与 Cursor 工作区 **Git 自动提交** 规则一致，每轮修改后：

1. 完成上述验证。
2. 执行者确认本轮目标达成（自用或 Code Review）。
3. 若有待提交变更：

```powershell
git add -A; git commit -m "[Cursor] <short English description of what changed>"
```

- 无变更则跳过 `commit`。
- Windows PowerShell 使用 `;` 串联命令。

## 5. 文档索引

完整列表见 [README.md](../README.md)。
