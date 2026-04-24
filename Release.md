# Release notes (from repository git tags)

Messages below are copied from each tag’s annotation and/or the tagged commit, as reported by `git show <tag>`.

Tags are listed in **version order** (`v0.0.1` → `v0.1.0-Connect-To-Backend` → `v0.2.0`).

---

## `v0.0.1`

**Tag type:** annotated tag

**Tag message (annotation):**

```
Connect to backend
```

**Tagged commit:** `36ff5f2c3be58787a2865962cbbe0fe72875d4f2`

**Commit message:**

```
[Cursor] Remove main screen chat section title and subtitle

Made-with: Cursor
```

---

## `v0.1.0-Connect-To-Backend`

**Tag type:** lightweight tag (points directly at a commit; no separate tag message)

**Commit:** `36ff5f2c3be58787a2865962cbbe0fe72875d4f2`

**Commit message:**

```
[Cursor] Remove main screen chat section title and subtitle

Made-with: Cursor
```

---

## `v0.2.0`

**Tag type:** annotated tag

**Tag message (annotation):**

```
SSE with backend server and TTS on Android side
```

**Tagged commit:** `57fc8eb42567cd1b3a0a8466041084121488f062`

**Commit message:**

```
[Cursor] feat(chat): Hermes chat via SSE stream with incremental UI

Made-with: Cursor
```

---

## `v0.3.0`

已合入 **`main`**；初版功能见 `63bd2b5`，后续修订见各条 commit。

- [x] 交付流程（Cursor）：特性合入后同步本小节与 `git` 说明；完整构建验证见 `.cursor/rules/assemble-apk-after-changes.mdc`；Release 条目写法与收尾步骤见 `.cursor/skills/herdroid-release-apk/SKILL.md`。
- [x] 文本朗读在独立线程编排，主线程仅做必要的 `TextToSpeech.speak` 等调用（详见代码注释）。实现 commit：`3b112e3`（`SystemTtsSpeaker` 后台预处理与分段朗读）
- [x] 边读边显示：三行歌词式弹窗，中间当前行粗体、向上滚动；暂停与退出。初版：`3b112e3`；修订：`1c71a6c`（`MessageSanitizer.forSpeechPreserveParagraphs`、三行可滚动全文、主线程 `onStart`、末段 `onDone` 后再关窗）
- [x] 统一收发消息折叠样式：一行内「前缀若干字符 + 展开」。实现 commit：`3b112e3`（`ChatUiStyle` / `collapsedPrefixPreview`）
- [x] 发送新消息后收起会话中所有已展开的气泡（`MainViewModel.collapseExpandEpoch`，入列前递增；各气泡 `LaunchedEffect` 将 `replyExpanded` 置为 `false`）。实现 commit：含本功能的提交（message：`feat(chat): collapse all expanded bubbles when sending`）
- [x] 接收回复流式阶段：单行展示并以总长度更新，减少界面跳动。初版：`3b112e3`（`ChatBubble` 单行摘要）；文案修订：`6a14c26`（仅一行 **`【接收中】已经接收%1$d字符`**，`chat_streaming_receiving`）
- [x] 回复消息快捷菜单增加「展开说说」。实现 commit：`3b112e3`（菜单项 + `scrollToItem`）
- [x] 所有消息快捷菜单「选择」：弹出对话框，内嵌系统可选中文本（`TextView` + `ScrollView`），长按或拖选后用系统工具栏复制。实现 commit：含 `feat(chat): message selection dialog with selectable TextView` 的提交
- [x] 回复消息按 Markdown 格式显示（展开后）。实现 commit：`3b112e3`（`multiplatform-markdown-renderer` 0.27 + `AssistantMarkdownBody`）

---

## `v0.4.0`

计划中；以下为 **v0.4.0** 修改清单（交付后将对应条目标为 `- [x]` 并补充实现 commit / PR）。标题前缀与 GitHub Issue 建议标题一致：`[doc]` 一条、`[feature]` 五条。

- [x] **\[doc]** 翻译英文 `README.md`，并作为默认入口；原中文说明保留为中文版；中英文两个 README 互相链接。实现 commit：与 `[Cursor] docs: English default README` 同批提交。
- [x] **\[feature]** 支持多个设置文件（profile），用户可在不同 profile 间切换，便于对接不同 Hermes agent 实例。实现 commit：（与 `[Cursor] feat(settings): per-profile DataStore keys` 同批提交）
- [x] **\[feature]** 折叠消息时：若原文长度与 trunk 后长度相同，则不必提供折叠态（不显示折叠）。实现 commit：（与 `[Cursor] ui(chat): skip fold when preview equals full text` 同批提交）
- [x] **\[feature]** 正在播放语音时若收到新的回复：将新回复的朗读加入**待播放队列**，当前段播完后再播下一段，直至队列为空。实现 commit：（与 `[Cursor] feat(tts): queue speak requests while playing` 同批提交）
- [x] **\[feature]** 梳理并加固现有逻辑：播放语音期间若用户切换到输入法，则暂停播放；在消息已发送或用户退出输入法后，再继续播放。实现 commit：（与 `[Cursor] feat(main): IME visibility pauses TTS` 同批提交）
- [x] **\[feature]** 每次发送或收到消息后记录当前对话的 session id，以便下次继续同一对话；下次启动 app 时询问用户是**继续上次对话**还是**开启新对话**。实现 commit：（与 `[Cursor] feat(chat): persist session and resume prompt` 同批提交）
- [x] **\[feature]** 长耗时会话改为 Runs API：发送时先创建 `run_id` 再订阅 `/v1/runs/{run_id}/events`，避免单次 `chat/completions` 长连接在 10+ 分钟场景下更易断连；并兼容解析 chat-completions / responses 两类增量事件。实现 commit：（与 `[Cursor] feat(chat): switch main conversation to Hermes Runs API` 同批提交）
- [x] **\[feature]** Runs 长会话断线自动重连：在同一 `run_id` 上自动续订阅事件流，新增重连提示文案「连接已恢复，继续接收中…」，并对重连回放增量去重以避免重复字符。实现 commit：（与 `[Cursor] feat(chat): reconnect runs stream on disconnect` 同批提交）
- [x] **\[feature]** Runs 重连策略可配置并支持手动续订阅：设置页新增「自动重连次数（0-10）」；自动重连耗尽后，主界面弹窗可继续订阅同一 `run_id` 或停止等待。实现 commit：（与 `[Cursor] feat(chat): configurable and resumable runs reconnect` 同批提交）
- [x] **\[feature]** 手动恢复入口增强：当 Runs 重连耗尽后可选择「稍后」，并在助手消息长按菜单中随时点击「继续接收」恢复同一 `run_id` 订阅。实现 commit：（与 `[Cursor] feat(chat): resume runs stream from message menu` 同批提交）

### 今日提交归类（2026-04-24）

- **聊天交互（长按菜单与折叠）**
  - `3a82350`：消息长按菜单新增折叠切换入口，并将主界面「新对话」改为 `+` 图标。
  - `3d24166`：统一所有消息长按菜单增加「折/展」，按当前状态在折叠与展开间切换。
  - `ed797be`：当助手消息仍为 0 字符时，长按菜单新增「刷新」，可主动续拉该消息对应的后台回复流。
  - `7b258f2`：修复「刷新」前置链路：可恢复失败时保留 0 字符助手消息，确保可长按并触发主动续拉。
- **设置页交互优化（Profile 与布局）**
  - `6685af7`：profile 改为下拉即切换；新增删除 profile，带不可恢复提示与二次确认；新建/删除同一行。
  - `2aa76c6`：压缩设置表单布局：`协议 + 地址 + 端口` 同行，`API Key + 模型名称` 同行。
  - `65380b6`：设置项文案更新：`访问协议` -> `协议`，`地址（IP 或主机名）` -> `域名/IP`。
  - `3e4d6a0`：设置页允许「模型名称」为空（不再自动回退默认模型名）。
  - `bcd740d`：将「Runs 自动重连次数」改为固定下拉选项（`0/1/3/6/10`）。
- **流程规则**
  - `0d80806`：新增规则 `report-apk-full-name-after-build.mdc`，要求每次 APK 构建后汇报 APK 完整文件名（优先完整路径）。
  - `d76af3e`：禁用“修改后自动构建 APK”的自动应用规则（改为手动触发构建）。
- **发布文档维护**
  - `6fe091c`：将当天提交按主题归类追加到 `v0.4.0` 小节（本次已在该基础上增量更新并去重）。
  - `894172b`：继续完善 `v0.4.0` 归类内容，并新增可复用 skill：`summarize-commits-to-release`（支持按最近 N 天整理提交）。

---

## `v0.4.2`

基于 `docs/v0.4.2-plan.md` 的实施批次。执行规则：每项完成后立即提交 git，并在本节同步勾选与补充 commit。

- [x] A1 消息状态可视化统一（接收中/待恢复/已恢复/失败）。实现 commit：`d5d3b2b`
- [x] A2 长按菜单重排分组（会话控制、内容操作、其他）。实现 commit：`505302b`
- [ ] A3 输入区可用性增强（发送状态反馈与提示优化）
- [ ] B4 设置页信息分区（连接配置、模型与鉴权、稳定性）
- [ ] B5 Profile 操作反馈补强（切换/删除后的明确提示）
- [ ] B6 术语统一（刷新/继续接收/重发等口径）
- [ ] C7 微交互一致性（菜单、折展、状态切换动画）
- [ ] C8 无障碍与触达优化（点击区域、语义、对比度）
- [ ] C9 空状态与异常态增强（给出下一步行动）
- [ ] 全部完成后执行一次 `assembleDebug`，并汇报 APK 完整文件名与完整路径。

---

## License

This project is released under the **MIT License** already in the repository: see [`LICENSE`](LICENSE) at the repo root (Copyright © 2026 Vincent Liang).
