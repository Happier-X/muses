# 组件库迁移 Konsta UI — 执行计划

## 阶段 0：基础设施（验证点：构建 + 首页渲染）

- [ ] 0.1 `npm i konsta`（v5.3.0）
- [ ] 0.2 主题入口：`src/theme/tailwind.css` 追加 `@import 'konsta/vue/theme.css'`（与 happier-ui styles 共存，命名空间不冲突：--h-* vs --k-*/--color-ios-*；`src/theme/tokens.css` 暂留）。共存期 body 样式暂用 --h-*，阶段 3 切 Konsta
- [ ] 0.3 新增 `src/composables/useSystemDark.ts`（matchMedia 监听 prefers-color-scheme → 根元素 `.dark` class，初始化即同步），在 `main.ts` 调用
- [ ] 0.4 重构 `src/components/ui/index.ts`：新增 k* 组件 re-export（kButton、kNavbar、kList、kListItem、kToggle、kPopup、kSheet、kActions、kDialog、kToast、kCard、kCheckbox、kProgressbar、kTabbar、kTabbarLink、kListInput、kPage、kBlock、kBlockTitle、kFab 等），保留 MCover/MPage/MContent
- [ ] 0.5 App.vue 根 div 替换为 `<k-app theme="ios" class="flex flex-col h-full overflow-hidden">`（官方最佳实践：纯 Konsta 应用用 k-app 包裹）；验证高度链不回归（design R1）
- [ ] 0.6 验证：`npm run dev` 首页正常、`npm run build` 通过

## 阶段 1：壳与导航（验证点：Tabs 切换 + 页面骨架）

- [ ] 1.1 TabsPage：HTabBar → k-tabbar + k-tabbar-link（图标用 lucide）
- [ ] 1.2 各页面 HNavBar → k-navbar（标题/返回），评估 k-page vs 保留 MPage 壳（见 design R1）
- [ ] 1.3 验证：页面骨架、tab 切换、返回手势无回归

## 阶段 2：逐页迁移（由简到难，每页验证后进下一页）

- [ ] 2.1 SettingsPage（k-list/k-list-item + k-toggle + k-toast）— 最简单，先行验证列表/开关/toast 组合
- [ ] 2.2 AlbumsPage / ArtistsPage（列表 + 空状态自建）
- [ ] 2.3 LibraryDetailPage（列表 + fab 悬浮球评估，design R3）
- [ ] 2.4 PlaylistsPage / PlaylistDetailPage（列表 + k-sheet + k-dialog + k-list-input）
- [ ] 2.5 SourcesPage（卡片 + 输入 + checkbox + progressbar + toast）
- [ ] 2.6 SongsPage（虚拟列表 + 底部弹层 + 输入 + 悬浮球）
- [ ] 2.7 QueuePage（k-popup + 列表）
- [ ] 2.8 PlayerPage 自绘区（控制按钮 → k-button 圆形变体、进度条、action sheet 样式；歌词/PIXI 不动，design R4）

每页验收清单：亮/暗色均正常、交互等价、无 console 报错、滚动/弹层流畅

## 阶段 3：清理与收尾（验证点：完整构建 + 真机）

- [ ] 3.1 全仓 grep 清理：`happier-ui`、`--h-`、`--muses-`、`h-icon`、`H[A-Z]` import 残留
- [ ] 3.2 删除 `src/theme/tokens.css` 与 `happier-ui` 相关 import；`npm uninstall happier-ui`
- [ ] 3.3 `npm run build`（vue-tsc + vite build）+ `npm run lint` 全通过
- [ ] 3.4 Android 模拟器冒烟：亮/暗色切换、滚动、弹层/action sheet、MiniPlayer、播放队列、悬浮球（design 真机清单）
- [ ] 3.5 视觉走查：全部 11 页面截图对比迁移前后

## 验证命令

```bash
npm run dev        # 开发预览
npm run build      # vue-tsc 类型检查 + vite 构建
npm run lint       # eslint
# Android：npm run android 或 npx cap run android
```

## 风险文件 / 回滚点

- `src/theme/tailwind.css`、`src/theme/tokens.css`（主题切换核心，阶段 0 首验）
- `src/components/ui/index.ts`（组件出口，重构影响所有页面）
- `src/App.vue`（根 class + provider + 遮罩联动）
- `src/views/PlayerPage.vue`、`src/components/MiniPlayer.vue`（自绘改造，成本最高）
- 每阶段独立 commit，可单独 revert；卸载 happier-ui 是最后一步，回滚安全

## 依赖与顺序约束

- 阶段 0 → 1 → 2 严格串行（壳先行，页面依赖壳）
- 阶段 2 页面间无强依赖，但按复杂度和组件复用顺序推进（2.1 先行验证通用组合）
- PlayerPage（2.8）依赖 2.1 验证的 k-button/k-actions 模式，放最后
