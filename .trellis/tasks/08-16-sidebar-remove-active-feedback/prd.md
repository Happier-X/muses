# PRD：侧边栏菜单取消点击背景效果

## 背景

用户反馈：点击专辑/艺术家/歌单等菜单项出现蓝色按压背景，点击歌曲（当前激活项）没有——不一致，要求统一取消点击效果。

根因：`.tabs-layout__nav-link` 的 `&:active { background-color: rgba(var(--m-primary-rgb), 0.08) }` 按压反馈仅对非激活项生效（激活项 `--active` 的 `background-color: transparent` 定义在后、同特异性覆盖了 :active）。

## 需求（仅 TabsPage.vue）

1. 删除 `&:active` 按压背景规则（nav-link 与 drawer-link 都不再有按压背景）
2. 保留：激活仅文字加粗、图标灰色不变

## 验收

- [ ] 六个菜单项点击均无背景变化
- [ ] MuMu 真机验证
- [ ] lint / type / build 通过
