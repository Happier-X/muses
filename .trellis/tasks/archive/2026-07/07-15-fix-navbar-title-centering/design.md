# navbar 标题统一居中技术设计

## 方案边界

在全局主题样式 `src/theme/variables.css` 中定义普通 navbar 标题布局规则，目标选择器限定为 `ion-header ion-toolbar > ion-title:not([size="large"])`。不逐页复制定位样式，不修改页面模板或按钮结构。

## 布局机制

- 将普通标题相对 `ion-toolbar` 绝对定位，横向覆盖 toolbar，使中心点不受 flex 中左右插槽宽度影响。
- 使用对称的左右内边距为操作区保留安全空间；标题内部沿用 Ionic Shadow DOM 的单行省略行为。
- 标题层禁用指针事件，左右 `ion-buttons` 保持 Ionic 既有高层级和点击能力。
- 显式排除 `[size="large"]`，保证折叠大标题保持文档流、左对齐及 Ionic 动画。

## 兼容性

- Material Design：补齐其默认 flex 标题不能真正居中的缺口。
- iOS：与 Ionic 自带绝对居中语义一致，统一的安全空间与排除规则不改变大标题。
- 宽屏：仍相对当前页面 toolbar 居中；平板侧栏不属于 toolbar 宽度，不引入额外偏移。

## 取舍

- 选择全局规则而非逐页 class：所有现有及未来 navbar 标题保持一致，避免遗漏 Modal / Overlay。
- 选择固定对称安全空间而非运行时测量两侧按钮：当前操作区均为单个图标或“关闭”文字按钮，CSS 足够；无需引入 ResizeObserver 和脚本状态。
- 长标题优先省略而不是动态偏离中心：保障操作按钮可用性和全局视觉一致性。

## 回滚

删除新增的全局普通标题定位规则即可恢复 Ionic 默认布局，不涉及数据或组件 API 迁移。
