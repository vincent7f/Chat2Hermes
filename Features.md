# Herdroid 功能点清单

本文档根据当前 **`main`** 代码与 [`Release.md`](Release.md)、[`README.md`](README.md)、[`docs/PRD.md`](docs/PRD.md)、[`docs/Ideas.md`](docs/Ideas.md) 整理，作为产品功能的**一览表**；细节与验收以各文档及实现为准。

---

## 1. 应用结构

| 能力 | 说明 |
|------|------|
| 主界面 | 多轮对话、输入框、顶部栏（自动朗读开关、设置、新对话） |
| 设置页 | 从主界面进入；配置保存于 DataStore |
| 主题 | Material 3 / `HerdroidTheme`，边到边（edge-to-edge） |
| 键盘 | `SOFT_INPUT_ADJUST_RESIZE`，输入区随软键盘调整 |

---

## 2. 连接与配置（设置）

| 功能点 | 说明 |
|--------|------|
| 访问协议 | `http` / `https`（含从 ws/wss 归一） |
| 主机与端口 | 局域网 IP 或主机名；端口校验 1–65535 |
| API Key | `Authorization: Bearer …`，与对话共用 |
| 模型名称 | OpenAI 兼容 `chat/completions` 所用模型 |
| 对话根地址 | 由协议 + 主机 + 端口拼接，不设第二条持久化 URL |
| 自动朗读开关 | 收到**完整**助手回复后是否自动调用系统 TTS（主界面开关，持久化） |
| 测试连接 | 对拼接后的服务根地址做健康检查类请求（见实现与文案） |
| 测试对话 | 使用当前配置发简短对话请求并展示结果或错误 |
| 保存 | 写入 DataStore，供主界面与接口客户端读取 |

**相关代码**：`SettingsRepository`、`UserPreferences`、`SettingsScreen`、`SettingsViewModel`、`HealthCheckUrlFactory`

---

## 3. 对话与网络（主界面）

| 功能点 | 说明 |
|--------|------|
| OpenAI 兼容对话 | `POST …/v1/chat/completions`，长读超时（适配 Hermes 工具调用等长耗时） |
| SSE 流式增量 | `stream: true`，增量合并到当前助手气泡；跳过非标准 Hermes 事件行 |
| 发送前校验 | 无网络、配置不完整等给出 Snackbar/提示文案 |
| 用户消息状态 | 发送中 / 成功 / 失败与图标展示 |
| 失败处理 | 失败时更新气泡状态并提示；可选移除无内容的助手占位 |
| 新对话 | 清空当前会话消息列表 |

**相关代码**：`MainViewModel`、`OpenAiChatClient`、`OpenAiChatFromSettings`、`OkHttp`（`HerdroidApplication`）

---

## 4. 消息展示与样式

| 功能点 | 说明 |
|--------|------|
| 时间戳 | 相邻消息间隔 ≥ 60s 时显示 `[HH:mm]` 行 |
| 流式进行中 | 单行 **`【接收中】已经接收 N 字符`**，内部仍累积全文 |
| 折叠样式（可配） | 开启「自动朗读」时，用户/助手已完成消息可折叠为**前缀 + 展开**（`ChatUiStyle` / `collapsedPrefixPreview`） |
| 助手回复 Markdown | 展开后或非折叠场景下，对正文使用 Markdown 渲染（依赖 `multiplatform-markdown-renderer`） |
| 发送后收起 | 新一轮发送入列前递增 epoch，所有气泡的「展开」状态统一收起 |

---

## 5. 交互与快捷菜单（气泡长按）

| 功能点 | 用户消息 | 助手消息 |
|--------|----------|----------|
| 朗读 | ✓ | ✓ |
| 选择 | ✓ 弹出系统可选中 `TextView`，支持复制选段 | 同左 |
| 重发 | ✓ | — |
| 展开说说 | — | ✓ 展开并滚动到该条 |
| 复制全文 | — | ✓ |

**音量**：媒体音量过低时 TTS 前可显示顶部浮窗提示。

---

## 6. 文本朗读（TTS）

| 功能点 | 说明 |
|--------|------|
| 后台预处理 | 清洗、分段等在后台线程；`TextToSpeech` 构造与 `speak` 在主线程（见类注释） |
| 歌词式弹窗 | 三行预览（上/中粗/下）、段进度、暂停 / 继续 / 退出 |
| 段落保留 | 朗读用 `MessageSanitizer.forSpeechPreserveParagraphs` 保留换行 |
| 触发 | 自动朗读（开关打开）与长按「朗读」均走分段歌词流程（实现以代码为准） |

**相关代码**：`SystemTtsSpeaker`、`MainViewModel`、`MainScreen`（`TtsLyricDialog`）

---

## 7. 构建与工程约定

| 功能点 | 说明 |
|--------|------|
| Debug APK | `assembleDebug`；可归档为带时间戳文件名并复制到配置目录（见 `app/build.gradle.kts`） |
| Release 追踪 | 版本级说明见 [`Release.md`](Release.md)；交付时同步 checklist 与构建验证见 [`.cursor/skills/herdroid-release-apk/SKILL.md`](.cursor/skills/herdroid-release-apk/SKILL.md) |

---

## 8. 与版本标签的对应关系（摘要）

| 标签 / 段落 | 内容摘要 |
|-------------|----------|
| **v0.2.0** | SSE 流式对话、增量 UI |
| **v0.3.0**（见 `Release.md`） | TTS 与歌词弹窗、统一折叠、流式单行文案、展开说说、Markdown、选择文本、发送后收起、交付流程与 skill 等 |

---

## 9. 已知文档差异说明

- [`docs/PRD.md`](docs/PRD.md) 部分历史条款写「不包含 TTS」；**当前应用已包含系统 TTS 与歌词弹窗**，以代码与 `Release.md` 为准。
- 更长连接（如独立 WebSocket 常驻会话）若在 PRD 开放问题中讨论，**主对话路径以 HTTP + SSE 实现为准**；代码库中另有 WebSocket 相关模块，是否暴露为产品功能以实际界面为准。

---

*文件版本：与仓库 `Features.md` 同迭代更新。*
