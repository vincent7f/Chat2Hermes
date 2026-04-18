# Android 工程约定 — Herdroid

本文档约定 SDK 版本、清单、网络安全与后台行为边界，与 [ARCHITECTURE.md](ARCHITECTURE.md) 配合使用。Gradle 工程落地后，具体数值应在 `build.gradle` / `libs.versions.toml` 中可查，并与本文档保持一致。

## 1. SDK 与构建

| 项 | 原则 | 初值（工程就绪后填入实际） |
|----|------|---------------------------|
| `compileSdk` | 使用当前稳定最新或次新版本 | 待填 |
| `targetSdk` | 与 Google Play 要求对齐，每年随政策升级 | 待填 |
| `minSdk` | 在设备覆盖与 API 特性间权衡；Compose Material3 需满足官方最低要求 | 建议 ≥ 24（以 Android Gradle Plugin 文档为准） |

- **Java/Kotlin 目标版本**：与 AGP 默认推荐一致（如 JVM 17）。
- **构建变体**：至少 `debug` / `release`；`release` 须启用 R8/混淆与资源收缩时再单独文档化规则。

## 2. 应用清单（AndroidManifest）

- **Application 类**：若使用 Hilt 等，注册 `android:name`。
- **单 Activity**：`MainActivity` 为 `exported` 启动入口；其余组件非必要不 `exported`。
- **权限最小化**：
  - `INTERNET`：连接 HA 与网络 TTS 时必需。
  - **不**默认申请麦克风、位置、通讯录等与 PRD 无关权限。
- **局域网 HTTP**：若 HA 仅提供 `http://`，必须通过 **Network Security Config** 或明确开发策略处理，禁止在发布版中无限制 `usesCleartextTraffic="true"` 而不加域限制（见下节）。

## 3. 网络安全配置（Network Security Config）

- **明文 HTTP（常见于局域网）**：推荐为**指定域名/IP** 开启 cleartext，而非全局放开。在 `res/xml/network_security_config.xml` 中为 `HA` 主机配置 `cleartextTrafficPermitted` 或调试专用 `debug-overrides`。
- **HTTPS + 自签名/私有 CA**：使用 `networkSecurityConfig` 绑定证书或用户导入信任（若产品允许）；文档化运维步骤，避免硬编码私钥。
- **文档要求**：仓库中保留 `network_security_config` 示例与注释，说明「生产 vs 调试」差异。

## 4. 本地网络与发现（前瞻）

- Android 12（API 31）起对**本地网络**访问有更细粒度说明；若未来增加「扫描局域网 HA」或 mDNS，可能涉及 `NEARBY_WIFI_DEVICES` 或局部网络权限（随版本查阅官方文档）。
- **当前 PRD** 以手动输入地址为主；未实现扫描前不在清单中预申请额外权限。

## 5. 后台与连接保活

- **默认**：应用在前台时保持连接即可满足 MVP；用户切到后台时可断开或短时保持（产品决策）。
- **若需后台常驻**：评估 **Foreground Service**（须符合 Google 前台服务类型政策）、持久通知与用户可取消性；禁止滥用 `SCHEDULE_EXACT_ALARM` 等与场景无关 API。
- **WorkManager**：适用于「可延迟、可重试」任务，不替代实时双向流；HA 长连接仍以合适生命周期作用域管理。

## 6. 可访问性与国际化

- 布局与 Compose 语义见 [UI_UX.md](UI_UX.md)；字符串资源外置 `strings.xml`，便于翻译。
- **RTL**：Compose Material 默认支持镜像；自定义绘制需注意。

## 7. 签名与发布

- `release` 使用独立 keystore；密钥不入库。CI 若存在，使用加密密钥或密钥管理服务。

---

约定变更时更新本文件并在 [DEVELOPMENT.md](DEVELOPMENT.md) 中同步验证命令。
