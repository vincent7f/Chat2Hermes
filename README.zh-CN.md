# Herdroid

**Herdroid** 是 Hermes Agent（文档内简称 **HA**）的 Android 客户端：在局域网内与 HA 建立连接，并通过 OpenAI 兼容 API 进行文本对话。

**Languages:** [English README](README.md)

## 文档索引

| 文档 | 说明 |
|------|------|
| [Features.md](Features.md) | 功能点清单（与代码、Release 对齐） |
| [docs/Ideas.md](docs/Ideas.md) | 原始设想与目标 |
| [docs/PRD.md](docs/PRD.md) | 产品需求与验收标准 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 架构与模块划分 |
| [docs/ANDROID_CONVENTIONS.md](docs/ANDROID_CONVENTIONS.md) | Android 工程与安全网络等约定 |
| [docs/UI_UX.md](docs/UI_UX.md) | UI/UX 与无障碍 |
| [docs/PRIVACY_AND_SECURITY.md](docs/PRIVACY_AND_SECURITY.md) | 隐私与安全 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 构建、验证与 Git 工作流 |
| [docs/HERMES_API_SERVER.md](docs/HERMES_API_SERVER.md) | Hermes API Server 官方说明与客户端约定（超时、鉴权等） |

## 功能摘要

- 与同局域网内的 HA 保持连接；在设置中配置访问协议、主机地址、端口，以及 API Key、模型名（对话根地址由前三项自动拼接）。
- 主界面多轮文本对话（`POST …/v1/chat/completions`，SSE 流式、`stream: true`）。

## 构建与验证

在项目根目录配置 `local.properties`（`sdk.dir` 指向本机 Android SDK），然后：

```powershell
.\gradlew.bat lint assembleDebug
```

构建完成后会生成默认包 `app/build/outputs/apk/debug/app-debug.apk`，并可能复制为带时间戳的 **`Herdroid-debug-<yyyyMMdd-HHmmss>.apk`**（见 `archiveHerdroidDebugApk`）。详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 许可

本项目采用 **MIT License**，见仓库根目录 [LICENSE](LICENSE)。
