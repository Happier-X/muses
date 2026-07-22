# 设计系统 Token 化与 Muses UI 组件库

## Goal

把 Muses 的视觉决策从页面硬编码收敛为可复用的 **设计 token**，并在 token 之上建立 **薄语义组件层**（二次使用 Ionic，而非重写控件引擎），使列表/空状态/封面等模式全项目一致，并与 `PRODUCT.md` / `DESIGN.md` 对齐。

## Background

- 已有：`PRODUCT.md`（product / android / 非 Material）、`DESIGN.md`（暗场听席、flat、HeroUI primary `#006FEE`）
- 已有：`src/theme/variables.css` 中 HeroUI primary 与布局断点
- 缺口：全局语义 token 不足；`src/components` 几乎无 UI 库；`empty-state` / 封面 / 列表高亮等多处复制
- 反模式：不要为每个 `ion-*` 做 1:1 同名封装

## Scope（已锁定）

- **档位 B**：Token 化 + 第一批语义组件，并迁移高频列表页
- **目录**：`src/components/ui/`
- **组件**：`MEmptyState` + `MCover` + `MPage`；列表行按成本做薄封装或共享样式
- **顺序**：先归档 `07-22-impeccable-init`，再 `task.py start` 本任务

### In scope

**阶段 A — Token 化**

- 新增 `src/theme/tokens.css`：颜色（primary 50–900、中性、immersive）、间距、圆角、字号角色、动效、z-index、断点
- `variables.css` 作 Ionic 桥接：`--ion-color-primary*` 等指向 Muses token
- 高频硬编码 → `var(--muses-*)`（封面圆角、空状态、播放高亮、平板 max-width、MiniPlayer/Tab 间距等）
- 同步 `DESIGN.md`（及必要时 sidecar）token 对照

**阶段 B — 语义组件 + 迁移**

| 组件 | 用途 |
|------|------|
| `MEmptyState` | 曲库/歌单/音源等空列表 |
| `MCover` | 列表与 MiniPlayer 封面/占位 |
| `MPage` | `ion-page` + 无阴影 header 约定 + content 槽 |
| 列表行 | 共享样式或薄 `MSongRow`（实现时按成本取舍） |

- 迁移：Songs / Albums / Artists / Playlists（empty/cover/page 壳优先）；同类模式顺带收敛
- 更新 `.trellis/spec/frontend`（directory / component / 可选 theme 约定）

### Out of scope

- 全量 Ionic 1:1 封装
- PlayerPage 大拆组件（仅 token 对齐色/动效）
- Storybook / 新 CSS 框架
- 播放/扫描等非 UI 业务逻辑

## Constraints

- PRODUCT：稳先于炫；Android 交付、非 Material；主色 `#006FEE` 仅状态/主操作
- DESIGN：flat、暗场听席、HeroUI Primary Rule
- Vue 3 SFC + `<script setup lang="ts">` + 既有 Ionic 页面模式
- 触控与安全区不回退

## Acceptance Criteria

- [ ] 存在统一 token 源；primary 与 DESIGN 一致（`#006FEE` 及 50–900）
- [ ] Ionic primary 由 token/桥接驱动；深色不回退 Ionic 默认蓝
- [ ] 至少 3 类重复硬编码已 token 化
- [ ] `MEmptyState`、`MCover`、`MPage` 已交付，且 ≥2 个页面实际引用 Empty/Cover；≥1 页使用 MPage
- [ ] Songs / Albums / Artists / Playlists 完成 empty/cover（及可行的 page 壳）迁移
- [ ] 无新增 Material elevation / 运营卡片模式
- [ ] lint / type-check 通过
- [ ] frontend spec 已记录 token 与 `components/ui` 约定
- [ ] `07-22-impeccable-init` 已归档后再开始本任务实现

## Notes

- 复杂任务：须 `design.md` + `implement.md`，审阅后再 `start`
- 依赖：`PRODUCT.md`、`DESIGN.md`、`.trellis/spec/frontend/*`
