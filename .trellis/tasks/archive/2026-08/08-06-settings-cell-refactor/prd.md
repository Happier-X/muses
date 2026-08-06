# 设置页改用 HCell 组件

## Goal

将设置页手写 HTML 结构改为使用 happier-ui 的 HCell / HCellGroup 组件，保持现有功能和外观语义不变。

## Requirements

- 使用 `HCellGroup` + `HCell` 重构 `src/views/SettingsPage.vue` 的两块信息区：
  - 关于区：Muses 标题 + 应用版本
  - 音量均衡：标题 + 描述 + `HSwitch`（放 `suffix` 插槽）
- 音量均衡的 `watch` 逻辑、检查更新按钮、toast 提示保持不变
- 遵循组件库现有 HCell API（title / description / suffix 插槽 / click）

## Constraints

- 不改动任何业务逻辑（版本检查、音量均衡持久化等）
- 不引入新的依赖

## Acceptance Criteria

- [ ] 设置页使用 HCellGroup / HCell 渲染信息项，不再使用手写 h2/p 结构
- [ ] 音量均衡开关仍可正常切换并持久化
- [ ] 检查更新按钮、toast 行为不变
- [ ] `npm run build`（vue-tsc + vite build）通过

## Notes

- 轻量任务，PRD-only，无需 design.md / implement.md。
