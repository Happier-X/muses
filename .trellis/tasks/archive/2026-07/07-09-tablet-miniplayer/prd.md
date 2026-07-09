# 平板 MiniPlayer 宽屏居中限宽

## 目标

在宽屏（≥768px）下，MiniPlayer 从横向全宽拉伸 `left:12px; right:12px` + 底部贴 Tab Bar 改为居中限宽 720px、`bottom` 不受 Tab Bar 影响。窄屏保持当前形态不变。

归属于父任务 `07-09-tablet-adapt` 子任务 F。

## 背景

- MiniPlayer 当前 `position: fixed` 贴底，`bottom: calc(64px + safe-area-bottom)`（64px 为 Tab Bar 高度）
- 宽屏下 Tab Bar 已被隐藏（侧栏导航），`bottom` 不需要再偏移 Tab Bar
- 宽屏下横向无限拉伸不美观，需加内容宽度限位与侧栏/列表保持一致

## 需求

| ID | 需求 |
|----|------|
| R1 | 宽屏 ≥768px 下 MiniPlayer `bottom` 调整为 `calc(12px + var(--ion-safe-area-bottom))`（不依赖 Tab Bar） |
| R2 | 宽屏下 MiniPlayer 居中限宽 720px（`left: auto; right: auto; width: 720px; max-width: calc(100vw - 24px)` 兜底窄屏安全） |
| R3 | 窄屏 <768px 下形态与改动前完全一致 |
| R4 | 不影响 `App.vue` 或其他文件 |

## 技术约束

- 只改 `src/components/MiniPlayer.vue` 的 `<style scoped>`，不改模板/脚本
- 用 `@media (min-width: 768px)` 限定，窄屏不做变更

## 验收标准

- [ ] 宽屏 ≥768px 下 MiniPlayer 居中显示，宽度 ≤720px
- [ ] 宽屏下底部不悬空在已隐藏 Tab Bar 位置，正常贴近底部
- [ ] 窄屏 <768px 下 MiniPlayer 与改动前一致
- [ ] `npm run build` 通过
- [ ] `npm run lint` 零错误