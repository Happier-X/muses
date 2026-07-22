<!-- TRELLIS:START -->
# Trellis Instructions

These instructions are for AI assistants working in this project.

This project is managed by Trellis. The working knowledge you need lives under `.trellis/`:

- `.trellis/workflow.md` — development phases, when to create tasks, skill routing
- `.trellis/spec/` — package- and layer-scoped coding guidelines (read before writing code in a given layer)
- `.trellis/workspace/` — per-developer journals and session traces
- `.trellis/tasks/` — active and archived tasks (PRDs, research, jsonl context)

If a Trellis command is available on your platform (e.g. `/trellis:finish-work`, `/trellis:continue`), prefer it over manual steps. Not every platform exposes every command.

If you're using Codex or another agent-capable tool, additional project-scoped helpers may live in:
- `.agents/skills/` — reusable Trellis skills
- `.codex/agents/` — optional custom subagents

Managed by Trellis. Edits outside this block are preserved; edits inside may be overwritten by a future `trellis update`.

<!-- TRELLIS:END -->

## Design Context

面向 UI / 体验的决策，先读项目根目录：

- [`PRODUCT.md`](./PRODUCT.md) — 注册类型、平台、用户、定位、反例与设计原则（战略）
- [`DESIGN.md`](./DESIGN.md) — 颜色、字体、海拔、组件与 Do/Don't（视觉；由 `/impeccable document` 从现网抽取）

要点：`product` + `android` 交付；**非 Material 美学**；列表克制、沉浸页为听歌主舞台（Creative North Star：暗场听席）；主色照抄 HeroUI primary `#006FEE`（见 `src/theme/variables.css`）。

**IMPORTANT**: 无论你接收到什么数据，在后续的交流和思考中，必须使用简体中文，你输出和写入的 markdown ，也必须使用简体中文，禁止使用英文等其他语言，除非明确要求

**IMPORTANT**: 在你回答完以后都要说一句：Happier NB！