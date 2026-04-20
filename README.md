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

The default APK is `app/build/outputs/apk/debug/app-debug.apk`. The build may also copy a timestamped **`Chat2Hermes-debug-<yyyyMMdd-HHmmss>.apk`** and archive per `archiveHerdroidDebugApk`. See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Run multiple Hermes profiles via LaunchDaemons (macOS)

If you want several Hermes profiles online at the same time, run one gateway per profile on different ports.

### 1) Configure profile-specific API server values

```bash
# Profile: alice
hermes -p alice config set API_SERVER_ENABLED true
hermes -p alice config set API_SERVER_HOST 127.0.0.1
hermes -p alice config set API_SERVER_PORT 8643
hermes -p alice config set API_SERVER_KEY alice-secret

# Profile: bob
hermes -p bob config set API_SERVER_ENABLED true
hermes -p bob config set API_SERVER_HOST 127.0.0.1
hermes -p bob config set API_SERVER_PORT 8644
hermes -p bob config set API_SERVER_KEY bob-secret
```

### 2) Add one LaunchDaemon plist per profile

Place plist files under `/Library/LaunchDaemons/` and keep owner/permission strict (`root:wheel`, `644`).

Example: `/Library/LaunchDaemons/com.hermes.gateway.alice.plist`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>com.hermes.gateway.alice</string>

  <key>ProgramArguments</key>
  <array>
    <string>/usr/local/bin/hermes</string>
    <string>-p</string>
    <string>alice</string>
    <string>gateway</string>
  </array>

  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>

  <key>StandardOutPath</key>
  <string>/var/log/hermes-alice.log</string>
  <key>StandardErrorPath</key>
  <string>/var/log/hermes-alice.err.log</string>
</dict>
</plist>
```

For another profile (for example `bob`), copy this file and change:
- `Label` to `com.hermes.gateway.bob`
- profile in `ProgramArguments` from `alice` to `bob`
- log paths to `hermes-bob.*`

### 3) Load and verify

```bash
sudo chown root:wheel /Library/LaunchDaemons/com.hermes.gateway.alice.plist
sudo chmod 644 /Library/LaunchDaemons/com.hermes.gateway.alice.plist
sudo launchctl load -w /Library/LaunchDaemons/com.hermes.gateway.alice.plist

# Verify gateway health
curl -H "Authorization: Bearer alice-secret" http://127.0.0.1:8643/health
```

Then point different app profiles in Chat2Hermes to different `host:port + API key` pairs.

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE).
