# Hermes Agent API Server（参考资料）

官方文档（持续更新以文档站为准）：

**[API Server | Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server?sharetype=wechat)**

## 与本客户端的关系

- Hermes 将 Agent 暴露为 **OpenAI 兼容 HTTP 接口**；Herdroid 使用 **`POST {baseUrl}/v1/chat/completions`**，请求体为 `model`、`messages`、**`stream: true`**，响应为 **`text/event-stream`（SSE）**。
- 客户端按行解析 `data: {json}`，从 `choices[0].delta.content` 拼接助手正文；`data: [DONE]` 结束。Hermes 自定义的 **`event: hermes.tool.progress`** 等非标准 `choices` 行会被忽略。
- 对话为 **无状态**：每一轮请求都在 `messages` 中携带完整历史（本应用已实现）。
- **鉴权**：与文档一致，使用请求头 `Authorization: Bearer <API Key>`（对应服务端 `API_SERVER_KEY` 等配置）。
- Agent 可能执行终端、文件、联网等 **耗时工具调用**，SSE 仍可能长时间无首个 token，因此 **HTTP 读超时** 必须显著长于普通 REST（见 `HerdroidApplication` 中对话专用 OkHttp 客户端）；健康检查等短请求仍使用默认较短超时的客户端。

## 主界面行为

- 发送后立即插入空的助手气泡，随 SSE **增量更新** 文本；成功后用服务端拼接结果再对齐一次状态（避免与 `Handler` 投递顺序竞态）。
- 设置页「测试对话」同样走 SSE，仅收集完整字符串后展示，无逐字 UI。
