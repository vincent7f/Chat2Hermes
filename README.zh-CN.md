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

构建完成后会生成默认包 `app/build/outputs/apk/debug/app-debug.apk`，并可能复制为带时间戳的 **`Chat2Hermes-debug-<yyyyMMdd-HHmmss>.apk`**（见 `archiveHerdroidDebugApk`）。详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 通过 LaunchDaemons 同时启动多个 profile（macOS）

如果你希望多个 Hermes profile 同时在线，可以为每个 profile 启动一个 gateway，并使用不同端口。

### 1）为每个 profile 配置独立 API Server 参数

```bash
# profile: alice
hermes -p alice config set API_SERVER_ENABLED true
hermes -p alice config set API_SERVER_HOST 127.0.0.1
hermes -p alice config set API_SERVER_PORT 8643
hermes -p alice config set API_SERVER_KEY alice-secret

# profile: bob
hermes -p bob config set API_SERVER_ENABLED true
hermes -p bob config set API_SERVER_HOST 127.0.0.1
hermes -p bob config set API_SERVER_PORT 8644
hermes -p bob config set API_SERVER_KEY bob-secret
```

### 2）在 `/Library/LaunchDaemons/` 为每个 profile 新建 plist

每个 profile 一份 plist，放在 `/Library/LaunchDaemons/`，并确保权限为 `root:wheel`、`644`。

示例：`/Library/LaunchDaemons/com.hermes.gateway.alice.plist`

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

新增 `bob` profile 时可复制上面的文件，并修改：
- `Label` 改为 `com.hermes.gateway.bob`
- `ProgramArguments` 内 profile 从 `alice` 改为 `bob`
- 日志路径改为 `hermes-bob.*`

### 3）加载并验证

```bash
sudo chown root:wheel /Library/LaunchDaemons/com.hermes.gateway.alice.plist
sudo chmod 644 /Library/LaunchDaemons/com.hermes.gateway.alice.plist
sudo launchctl load -w /Library/LaunchDaemons/com.hermes.gateway.alice.plist

# 验证服务健康状态
curl -H "Authorization: Bearer alice-secret" http://127.0.0.1:8643/health
```

最后在 Chat2Hermes 中创建多个连接 profile，分别填入对应的 `host:port + API Key` 即可动态切换后端 profile。

## 许可

本项目采用 **MIT License**，见仓库根目录 [LICENSE](LICENSE)。
