# Herdroid

**Herdroid** is an Android client for **Hermes Agent** (**HA** in docs): connect to HA on your LAN and chat via an OpenAI-compatible HTTP API.

**Languages:** [简体中文 README](README.zh-CN.md)

## Documentation index

| Document | Description |
|----------|-------------|
| [Features.md](Features.md) | Feature list (aligned with code and Release) |
| [docs/Ideas.md](docs/Ideas.md) | Original ideas and goals |
| [docs/PRD.md](docs/PRD.md) | Product requirements and acceptance |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture and modules |
| [docs/ANDROID_CONVENTIONS.md](docs/ANDROID_CONVENTIONS.md) | Android project and secure networking notes |
| [docs/UI_UX.md](docs/UI_UX.md) | UI/UX and accessibility |
| [docs/PRIVACY_AND_SECURITY.md](docs/PRIVACY_AND_SECURITY.md) | Privacy and security |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Build, verification, and Git workflow |
| [docs/HERMES_API_SERVER.md](docs/HERMES_API_SERVER.md) | Hermes API Server notes for clients (timeouts, auth, etc.) |

## Features (summary)

- Configure scheme, host, port, API Key, and model name in Settings (chat base URL is built from the first three).
- Multi-turn chat on the main screen (`POST …/v1/chat/completions`, SSE with `stream: true`).

## Build and verify

Create `local.properties` in the project root (`sdk.dir` pointing to your Android SDK), then:

```powershell
.\gradlew.bat lint assembleDebug
```

The default APK is `app/build/outputs/apk/debug/app-debug.apk`. The build may also copy a timestamped **`Herdroid-debug-<yyyyMMdd-HHmmss>.apk`** and archive per `archiveHerdroidDebugApk`. See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE).
