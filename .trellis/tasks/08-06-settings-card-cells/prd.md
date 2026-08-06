# 设置页 HCellGroup 改为卡片风格

## Goal

设置页的 HCellGroup 从默认 inset 形态改为卡片（`variant="card"`）风格。

## Background

- 组件库 happier-ui 0.1.1 的 `HCellGroup` 支持 `variant: 'card' | 'inset' | 'flat'`：card = 圆角（`--h-cell-group-radius`, 12px）+ 左右留白（`--h-cell-group-margin-x`, 16px）悬浮卡片，body 自带 `overflow:hidden` 保证 cell 圆角衔接。
- 设置页（`src/views/SettingsPage.vue`）当前有两个 `h-cell-group`（「关于」「音频」）均未传 variant，默认 inset（圆角无留白）。
- 页面容器 `space-y-[var(--muses-space-lg)]` 提供卡片间距；卡片左右留白 16px 与「检查更新」按钮容器 `p-[var(--muses-space-lg)]`（16px）天然对齐。

## Requirements

- R1：SettingsPage 两个 `h-cell-group` 均传 `variant="card"`，呈悬浮卡片风格。
- R2：现有内容（标题、说明、音量均衡开关、按钮）与布局不受影响。

## Acceptance Criteria

- [ ] AC1：设置页两个分组显示为卡片形态（圆角 + 左右留白），模拟器/构建产物确认。
- [ ] AC2：`npm run lint` 与 `npm run build` 通过。

## Out of Scope

- 其他页面 cell 样式调整。
- HCell 内部结构改动。

## Open Questions

- 无。
