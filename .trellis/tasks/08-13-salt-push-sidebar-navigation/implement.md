# 实施计划：推屏式 Salt 侧栏导航

## 实施步骤

1. 读取前端规范、当前 `TabsPage.vue`、MNavbar 抽屉上下文和 MiniPlayer/弹层层级，确认没有依赖覆盖式 backdrop 的调用方。
2. 重构 `TabsPage.vue` 移动端导航结构为 `50vw` 侧栏加 `100vw` 主页面的同层轨道；移除覆盖式固定抽屉与全屏遮罩。
3. 将打开、关闭、拖动、阈值收尾和取消路径统一绑定到轨道位移 `-50vw..0`，保留 Touch Events、方向锁、动画世代号和 reduced-motion 处理。
4. 调整打开态的 inert/aria/focus 行为：主页面视觉右移但不可点击，侧栏获得焦点；菜单选择、Escape、播放器/队列打开和路由变更均安全关闭。
5. 保持平板固定 260px 侧栏、四项导航数据、详情父入口高亮和 MNavbar 汉堡注入 API 不变。
6. 更新 `component-guidelines.md`：移动端导航从覆盖式抽屉契约改为 50vw 同层推屏轨道，并保留 WebView 110 Touch Events 约束。
7. 运行 `npm run build`、`npm run lint`、`git diff --check`、`npx cap sync android` 和 Android Debug 构建。
8. 在 MuMu WebView 110 通过 CDP/实际设备验证汉堡、右滑、左滑、纵滑、菜单导航、Escape、平板断点和无 JS/Vue/崩溃错误；明确记录不可验证的视觉像素项。
9. 全量复检、更新规范、提交代码和归档任务。

## 风险与检查点

- 轨道 transform 若施加到错误祖先，可能改变 fixed MiniPlayer/Popup 的 containing block；轨道只能包裹 `TabsPage` 的路由主内容，不能包裹 App 全局浮层。
- 页面使用绝对定位 `MPage`，移动端主页面容器必须保持明确高度、相对定位和 `overflow: hidden`，避免其在 `150vw` 轨道中水平溢出或失去滚动归属。
- 触摸收尾动画与 Escape/路由/overlay 关闭竞态必须继续用世代号隔离。
- 侧栏宽度为运行时 `viewportWidth * 0.5`，避免仅 CSS `50vw` 与 JS 阈值在横竖屏切换时不一致。

## 验证命令

```bash
npm run build
npm run lint
git diff --check
npx cap sync android
cd android && ./gradlew assembleDebug
```
