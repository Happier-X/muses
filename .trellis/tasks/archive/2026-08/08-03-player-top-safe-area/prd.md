# 沉浸式播放页顶部安全区适配

## Goal

沉浸式播放页（PlayerPage）在 edge-to-edge 设备上，顶部内容不再顶进状态栏；底部 FAB / panel 内边距同样遵守安全区。背景仍全屏贴边，仅内容层避让。

## Background

- PlayerPage 已迁到 `HPopup position="fullscreen"`；fullscreen panel 为 `inset:0`，**故意不加** safe-area（背景 edge-to-edge）。内容避让由宿主 `.player-overlay` 负责。
- Spec（`component-guidelines.md`）要求宿主不得只读 `env()`，须用三级回退：`var(--safe-area-inset-*, env(..., 0px))`（Capacitor 注入变量 → 标准 env → 0px）。Android WebView < 140 时裸 `env()` 可能为 0。
- 当前 Player 相关样式仍用裸 `env()`，且宽屏 media query 把 top safe-area 整段抹掉。

## Confirmed facts

| 位置 | 现状 | 问题 |
|------|------|------|
| `src/theme/tailwind.css` `.player-overlay .empty-state, .panel` 默认 padding | `calc(16px + env(safe-area-inset-top/bottom, 0px))` | 裸 `env()`，无 Capacitor 变量回退 |
| 同文件 `@media (min-width: 768px)` `.info-panel` | `padding: 24px` | **抹掉** top/bottom safe-area |
| 同文件 `@media (max-height: 720px)` `.info-panel` | `calc(10px + env(...))` | 裸 `env()` |
| 同文件 `@media (min-width: 768px) and (max-height: 720px)` | `padding: 16px 24px` | 抹掉 safe-area |
| 同文件 `@media (max-height: 520px)` | `calc(6px + env(...))` | 裸 `env()` |
| 同文件 `@media (min-width: 768px) and (max-height: 520px)` | `padding: 12px 20px` | 抹掉 safe-area |
| `src/views/PlayerPage.vue` 歌词 FAB | `bottom-[calc(8px+env(safe-area-inset-bottom,0px))]` | 裸 `env()` |
| HPopup fullscreen | 无 safe-area 内边距 | 符合设计，**不改库** |

## Requirements

1. **R1 三级回退**：Player 相关所有 `safe-area-inset-top/bottom` 改为 `var(--safe-area-inset-*, env(safe-area-inset-*, 0px))`。
2. **R2 断点不删安全区**：宽屏 / 矮屏 media query 只减小固定 px（16→10→6 等），始终 `calc(<固定px> + var(--safe-area-inset-*, env(...)))`；禁止纯 `padding: 24px` 这类抹掉安全区的写法。
3. **R3 歌词 FAB**：`PlayerPage.vue` 底部 floating actions 的 bottom 使用同样三级回退。
4. **R4 背景仍 edge-to-edge**：不给 HPopup panel / 背景层加 padding；只改内容 panel / empty-state / FAB。
5. **R5 不改 happier-ui**：本期不提库 issue、不升版本；fullscreen 无默认 safe-area 契约保持不变。

## Acceptance Criteria

- [ ] **AC1** 窄屏竖屏：控制页 / 歌词页 / 空状态顶部内容在状态栏下方，有可见留白（`16px + safe-area-top`）。
- [ ] **AC2** 宽屏（≥768px）：`info-panel` 仍保留 top/bottom safe-area（不再是纯 `24px`）。
- [ ] **AC3** 矮屏断点（720 / 520）与宽屏+矮屏组合：固定 px 可收紧，safe-area 分量仍在。
- [ ] **AC4** 歌词页底部 FAB 不与 Home Indicator / 底部系统手势条重叠。
- [ ] **AC5** AMLL / fallback 背景仍铺满全屏（含状态栏后方），无整体缩进。
- [ ] **AC6** `src/theme/tailwind.css` 与 `PlayerPage.vue` 的 Player 相关 safe-area 无裸 `env(safe-area-inset-*)` 单读（均带 `var(--safe-area-inset-*, …)`）。
- [ ] **AC7** `npm run lint` / 既有类型检查通过（无行为回归外的改动）。

## Out of Scope

- MiniPlayer / TabsPage / QueuePage / SongsPage 等其它裸 `env()` 全量扫修（可另开任务）
- 向 happier-ui 提 HPopup 非 fullscreen 位置的 safe-area 回退一致性 issue
- 改 HPopup fullscreen 契约、z-index、手势、keepAlive
- 状态栏颜色 / `syncPlayerStatusBar` 逻辑
- 视觉重排封面或控件尺寸（仅 padding 安全区，不改布局结构）

## Key Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 修复归属 | 宿主 Muses，不改库 | fullscreen 不应默认加 safe-area；内容层本就该自己避让 |
| 范围 | 仅 Player 相关 CSS/Vue | 对症、风险小；全局裸 env 另任务 |
| 回退公式 | `var(--safe-area-inset-*, env(..., 0px))` | 与 edge-to-edge spec / NavBar 正式实现一致 |
| 任务形态 | 轻量，PRD-only | 纯样式修补，无架构边界变化 |

## Risks

- 宽屏原先 `padding: 24px` 若设备 safe-area-top 很大，控制区会略变紧；可接受，且符合「不得抹掉安全区」spec。
- 仅改 CSS 时真机 WebView 行为需肉眼确认；桌面浏览器 safe-area 常为 0，无法完全替代真机。
