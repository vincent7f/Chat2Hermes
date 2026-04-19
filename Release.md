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

## `v0.3.0`（规划中）

功能追踪：完成后将 `- [ ]` 改为 `- [x]` 并填写实现 commit。

- [x] 文本朗读在独立线程编排，主线程仅做必要的 `TextToSpeech.speak` 等调用（详见代码注释）。实现 commit：`3b112e3`（`SystemTtsSpeaker` 后台预处理与分段朗读）
- [x] 边读边显示：三行歌词式弹窗，中间当前行粗体、向上滚动；暂停与退出。实现 commit：`3b112e3`（初版）；修订：保留分段换行（`forSpeechPreserveParagraphs`）、三行区可滚动展示全文、朗读进度回调切主线程、末段播完再关窗（见含 `fix(tts): lyric dialog` 的提交）。
- [x] 统一收发消息折叠样式：一行内「前缀若干字符 + 展开」。实现 commit：`3b112e3`（`ChatUiStyle` / `collapsedPrefixPreview`）
- [x] 接收回复流式阶段：单行展示并以总长度更新，减少界面跳动。实现 commit：`3b112e3`（`ChatBubble` 流式摘要行）
- [x] 回复消息快捷菜单增加「展开说说」。实现 commit：`3b112e3`（菜单项 + `scrollToItem`）
- [x] 回复消息按 Markdown 格式显示（展开后）。实现 commit：`3b112e3`（`multiplatform-markdown-renderer` 0.27 + `AssistantMarkdownBody`）
