# Herdroid

**Herdroid** 是 Hermes Agent（文档内简称 **HA**）的 Android 客户端：在局域网内与 HA 建立实时连接，接收消息并可选用系统或网络 TTS 播报。

## 文档索引

| 文档 | 说明 |
|------|------|
| [docs/Ideas.md](docs/Ideas.md) | 原始设想与目标 |
| [docs/PRD.md](docs/PRD.md) | 产品需求与验收标准 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 架构与模块划分 |
| [docs/ANDROID_CONVENTIONS.md](docs/ANDROID_CONVENTIONS.md) | Android 工程与安全网络等约定 |
| [docs/UI_UX.md](docs/UI_UX.md) | UI/UX 与无障碍 |
| [docs/PRIVACY_AND_SECURITY.md](docs/PRIVACY_AND_SECURITY.md) | 隐私与安全 |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | 构建、验证与 Git 工作流 |

## 功能摘要

- 与同局域网内的 HA 保持实时连接（具体协议见 PRD 开放问题）。
- 在设置中配置访问协议、主机地址与端口。
- 主界面一键开关：是否对 HA 返回内容自动 TTS 播报。
- TTS：系统 `TextToSpeech` 与可选网络 TTS 引擎，支持失败降级。

## 构建与验证

在项目根目录配置 `local.properties`（`sdk.dir` 指向本机 Android SDK），然后：

```powershell
.\gradlew.bat lint assembleDebug
```

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`。

详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 许可

待定（在添加 `LICENSE` 后更新本段）。
