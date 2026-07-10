# 修复沉浸式播放页底部导航残留

## 背景

用户反馈展开沉浸式播放页面时，底部仍然能看到主导航 tabbar。当前 `MiniPlayer` 已在 `/player` 和 `/queue` 页面隐藏，但 `TabsPage.vue` 的移动端底部导航仍可能作为父级 shell 残留在页面底部。

## 目标

- 进入 `/player` 沉浸式播放页时，不显示底部主导航 tabbar。
- 保持普通 tab 页面底部导航正常显示。
- 保持 `/queue` 页面不显示底部播放条，也不被 tabbar 干扰。
- 不重新引入 `ion-tabs`、`ion-tab-bar`、`ion-split-pane` 或 `ion-menu`。
- 移除沉浸式播放页顶部收起按钮，改为下滑手势收起。
- 下滑过程中页面需要跟随手指移动，松手达到阈值后返回上一页；未达到阈值则回弹。

## 范围

包含：

- 排查路由与 `TabsPage.vue` shell 布局导致的底部导航残留。
- 最小修改隐藏沉浸页下的底部 tabbar。
- 调整 `PlayerPage.vue` 的收起交互为下滑手势。
- 构建验证。
- 如需要，安装到 MuMu 验证。

不包含：

- 重构整体路由结构。
- 重新设计导航样式。
- 发布新版本。

## 验收标准

- [ ] `/player` 页面不显示底部主导航 tabbar。
- [ ] `/queue` 页面不显示底部主导航 tabbar。
- [ ] `/tabs/songs` 等普通 tab 页面底部导航仍显示并可点击。
- [ ] 底部 MiniPlayer 仅在非 `/player`、非 `/queue` 页面显示。
- [ ] `/player` 顶部不再显示收起按钮。
- [ ] `/player` 可通过下滑手势收起。
- [ ] 下滑过程中播放器页面跟随手指移动，未达到阈值时回弹。
- [ ] `npm run build` 通过。
