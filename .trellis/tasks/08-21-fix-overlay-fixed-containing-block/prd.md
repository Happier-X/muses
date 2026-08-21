# PRD：修复浮层组件因抽屉 track transform 导致 fixed 定位偏移

## 背景

用户反馈：在「音源」页点击「编辑」时，编辑音源对话框整体偏左（几乎贴屏幕左缘）。

## 根因

`src/views/TabsPage.vue` 的 `.tabs-layout__track` 是 `motion.div`，移动端抽屉关闭时始终带 `transform: translateX(-50vw)`。按 CSS 规范，祖先存在非 `none` 的 transform 时会成为 `position: fixed` 后代的包含块。因此页面树内所有 fixed 浮层改为相对 track 定位：track 宽 100vw（50vw 抽屉 + 50vw 主区）且左移 50vw，其 `left: 50%` 中点落在屏幕最左缘，导致对话框偏左。

受影响组件（均为 `position: fixed` 且渲染在页面树内）：

- `src/components/ui/MDialog.vue`
- `src/components/ui/MActions.vue`
- `src/components/ui/MSheet.vue`
- `src/components/ui/MPopup.vue`
- `src/components/ui/MToast.vue`

## 需求

1. 上述 5 个浮层组件模板最外层包 `<Teleport to="body">`，脱离 track 的包含块，恢复相对视口定位。
2. scoped 样式（data-v 属性）、z-index 阶梯（dialog/actions/sheet 1200、toast 1300、popup 1100）行为保持不变。
3. 动画（motion-v 进出场）保持正常。

## 约束

- 不改动 TabsPage 抽屉交互逻辑本身。
- 不引入新的全局样式 hack。

## 验收标准

1. 移动端视口下，「音源」页的编辑/删除/扫描设置对话框水平居中于视口。
2. 添加音源的 actionsheet、toast、播放页 popup 等浮层同样居中/位置正确。
3. 平板/桌面布局（无抽屉位移）下浮层表现不回归。
4. 浮层进出场动画正常，backdrop 点击可用。
5. lint / type-check / 既有测试通过。
