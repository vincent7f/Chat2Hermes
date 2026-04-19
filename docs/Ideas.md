# Hermes Agent 的安卓客户端

> 详细需求、架构与规范见下方文档索引；本页保留原始设想，为单一事实来源的补充说明。

## 文档索引

| 文档 | 说明 |
|------|------|
| [PRD.md](PRD.md) | 产品需求、验收标准、开放问题 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构与技术栈 |
| [ANDROID_CONVENTIONS.md](ANDROID_CONVENTIONS.md) | Android 工程与安全约定 |
| [UI_UX.md](UI_UX.md) | 界面与无障碍 |
| [PRIVACY_AND_SECURITY.md](PRIVACY_AND_SECURITY.md) | 隐私与安全 |
| [DEVELOPMENT.md](DEVELOPMENT.md) | 构建、验证与 Git 流程 |
| [../README.md](../README.md) | 项目简介与文档总览 |

## 目标

以下将 Hermes Agent 简称为 HA。

* 从安卓端实时连接同一局域网下的 HA。
* 可以在设置中配置本地的 HA 访问协议、地址和端口信息。
* 在主界面与 OpenAI 兼容 API 进行文本对话；服务基础 URL、API Key、模型名等在设置中配置。

## 非功能性要求

- 界面简洁、易用。
