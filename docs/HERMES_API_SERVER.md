# Hermes Agent API Server（参考资料）

官方文档（持续更新以文档站为准）：

**[API Server | Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server?sharetype=wechat)**

## 与本客户端的关系

- Hermes 将 Agent 暴露为 **OpenAI 兼容 HTTP 接口**；Herdroid 使用 **`POST {baseUrl}/v1/chat/completions`**，请求体为 `model`、`messages`、`stream: false`，与文档中的 Quick Start / curl 示例一致。
- 对话为 **无状态**：每一轮请求都在 `messages` 中携带完整历史（本应用已实现）。
- **鉴权**：与文档一致，使用请求头 `Authorization: Bearer <API Key>`（对应服务端 `API_SERVER_KEY` 等配置）。
- Agent 可能执行终端、文件、联网等 **耗时工具调用**，非流式模式下需等待服务端生成完整回复，因此 **HTTP 读超时必须显著长于普通 REST 接口**。本工程为「对话专用」的 OkHttp 客户端单独配置了较长的 **read timeout**（见 `HerdroidApplication`），健康检查等短请求仍使用默认较短超时的客户端。

## 可选后续能力（未实现）

- 文档中的 **`stream: true`（SSE）** 可降低首字等待时间并适合长回复；当前应用为非流式 JSON，若需可再接入解析 `chat.completion.chunk` 与 Hermes 的 `hermes.tool.progress` 等事件。
