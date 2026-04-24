---
name: summarize-commits-to-release
description: 按指定天数读取 git commit，去重后按主题归类写入 Release.md 的目标版本小节。用于“整理今天/最近N天提交并更新 release”类需求。
---

# Summarize Commits To Release

## 适用场景

- 用户要求“读取今天/最近几天的 commit 并写入 `Release.md`”。
- 用户要求“按主题归类、去重、增量更新 release 条目”。

## 输入参数

- `days`：最近几天（整数，默认 `1`）。
- `version`：目标版本标题（默认 `v0.4.0`）。
- `date_label`：小节标题日期文案（可选；默认使用当天日期）。

## 操作步骤

1. 获取提交：
   - 运行：`git log --since="<days> days ago" --pretty=format:"%H|%h|%ad|%s" --date=local`
2. 读取 `Release.md`，定位 `## \`<version>\`` 小节。
3. 查找或创建“提交归类”子节（例如“今日提交归类（YYYY-MM-DD）”或“最近N天提交归类（...）”）。
4. 按主题分组（建议：聊天交互、设置页优化、流程规则、文档维护等）。
5. 去重规则：
   - 若短哈希已存在：不重复添加；
   - 若同主题已有近似描述：合并为更完整描述，避免重复语义。
6. 写回 `Release.md`：
   - 仅修改目标版本小节；
   - 保持原有 Markdown 风格与排序。
7. 汇报结果：
   - 返回统计（读取到的提交数、新增条目数、更新条目数）；
   - 列出本次纳入的短哈希清单。

## 归类建议

- `feat(chat)`、消息菜单、折叠/展开、刷新/续拉 -> 聊天交互
- `feat(settings)`、`fix(settings)`、`refactor(settings)`、`chore(ui)` -> 设置页优化
- `chore(rule)` -> 流程规则
- `docs(...)` -> 文档维护

## 输出格式（写入 Release.md）

- 使用二级标题下的三级小节，示例：
  - `### 今日提交归类（2026-04-24）`
  - `### 最近3天提交归类（2026-04-22 ~ 2026-04-24）`
- 每个主题使用一条粗体分组，组内按 `- \`<short-hash>\`：说明`。
