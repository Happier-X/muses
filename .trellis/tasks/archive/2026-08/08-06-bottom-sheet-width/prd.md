# 修复 BottomSheet 面板宽度（slot-anchor 无样式导致变窄）

## Goal

修复 BottomSheet（歌单操作 sheet 等）在手机上不占满宽度、面板过窄的问题。

## Background（已实证，非猜测）

### 根因

HPopup（HBottomSheet 底层）渲染结构：

```
.h-popup（flex 容器，position-bottom：justify-content:center; align-items:flex-end）
  └─ .h-popup__slot-anchor（普通 div，**组件库无任何样式**）
       ├─ .h-popup__overlay（position:absolute; inset:0）
       └─ section.h-popup__panel（width:100%; max-width:var(--h-bottom-sheet-max-width,100%)）
```

组件库 `styles.css`（0.0.9 / 0.0.10）**都没有 `.h-popup__slot-anchor` 规则**。slot-anchor 作为 flex item（`flex: 0 1 auto`）宽度 = 内容 max-content（实测 133px），panel 的 `width:100%` 相对它解析为 133px（视口 360px）。

### 为什么 0.0.9 的「BottomSheet 默认全宽」未生效

commit 132ba49（跟进 Happier-X/happier-ui#14）只把 `--h-bottom-sheet-max-width` 默认值改为 100%——max-width 只是上限，**没有解决 slot-anchor 的宽度基准**，面板依旧按内容宽渲染。

### 实证（模拟器 WebView CDP）

- 打开歌单操作 sheet：容器 `.h-popup--position-bottom` 宽 360px（全宽），面板 133px（居中、窄）
- 匹配 panel 的 CSS 规则仅 3 条，`width:100%` 存在且无覆盖、无内联样式
- 手动 `panel.style.width='360px'` → 变 360px（flex 正常）；`'100%'` → 133px（解析基准是 slot-anchor）
- sheet 唯一子元素 `.h-popup__slot-anchor` 宽 133px

### 修复方案

项目已有组件库补丁先例（`.h-popup--position-fullscreen .h-popup__body { height:100% }`，tailwind.css）。按同样模式在 `src/theme/tailwind.css` 补：

```css
.h-popup--position-bottom .h-popup__slot-anchor,
.h-popup--position-top .h-popup__slot-anchor {
  width: 100%;
}
```

- 只作用于 bottom/top（sheet 类），**不影响** center（dialog 居中弹窗，内容自适应为设计行为）、left/right（面板固定 320px/75vw）、fullscreen、relative
- overlay 是 `position:absolute; inset:0`（相对 slot-anchor），slot-anchor 变宽后遮罩同步全宽，无冲突

### 最终范围（用户决策：只改组件库）

- **Muses 项目侧不做任何代码改动**（不添加 workaround，依赖组件库修复）。
- **组件库修复（已完成）**：相邻仓库 `../happier-ui` popup.css 合入补丁，commit `c4f119f` 已 push 到 https://github.com/Happier-X/happier-ui；`npm run build:lib` 后 dist/styles.css 确认含 `.h-popup--position-bottom/top .h-popup__slot-anchor { width:100% }`。
- **Issue（已完成）**：https://github.com/Happier-X/happier-ui/issues/15。
- **发布（留给后续）**：npm 上最新 0.1.0 不含此修复；相邻仓库有未提交的 HLoading 任务（08-06-hloading），发布新版本（如 0.1.1）前需先处理该工作区；Muses 升级依赖后 BottomSheet 恢复全宽。

## Requirements

- R1：组件库 `HPopup` bottom/top 的 slot-anchor 占满容器宽（修复上游，非 Muses 本地改动）。
- R2：不影响 dialog（center）等其他 position 的现有表现。
- R3（顺带）：确认 dialog 宽度正常（不受修复影响）。

## Acceptance Criteria

- [x] AC1：组件库 dist/styles.css 构建产物含 `.h-popup--position-bottom/top .h-popup__slot-anchor { width:100% }`（已验证）。
- [ ] AC2：Muses 升级含修复的组件库版本后，sheet 面板 = 视口宽（待发布后验证）。
- [ ] AC3：删除确认 dialog 仍为居中内容自适应弹窗（不变成全屏宽，待发布后验证）。
- [x] AC4：Muses 侧无代码改动（workaround 已撤销，git 干净）。

## Out of Scope

- 组件库上游修复（另行反馈 Happier-X/happier-ui）。
- 其他 position 的行为调整。

## Open Questions

- 无。
