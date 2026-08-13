# Implement: 复刻 Salt Player UI（视觉风格层）

> 实施状态：阶段 1-4 代码工作已完成；build/lint、静态检查和 MuMu APK 启动验证通过。MuMu 逐页视觉与完整业务流程需要在设备上继续人工走查。

## 执行顺序（每阶段独立提交，可单独回滚）

### 阶段 1：主题 token 迁移（`src/theme/index.scss`）
- [ ] 1.1 更新 `:root`：`--m-primary`→`#0470E6`、`--m-surface`→`#F3F3F3`、`--m-text`→`#1E1715`、`--m-text-2`→`#8C8C8C`、`--m-hairline`→stroke、subBackground 推导 surface-1/surface-2
- [ ] 1.2 更新 `.dark`：`--m-primary`→`#0088FF`、`--m-surface`→`#202020`、`--m-text`→`#EBEEF1`、`--m-text-2`→`#BFE1E6EB`、surface-1/2 推导、hairline→stroke@10%
- [ ] 1.3 新增 `--m-radius-*`（8/16/24/12/20px）、`--m-list-row-h:56px`、`--m-list-icon:24px`、`--m-spacing:16px`、`--m-spacing-sub:12px`
- [ ] 1.4 保留 `--m-safe-area-*`/`--m-danger`/`--m-success`/`.m-glass-*` 类不动
- [ ] 验证：`npm run build` + 启动 dev/MuMu 观察全局变色（可能先有页面不协调，属预期）

### 阶段 2：组件视觉对齐（28 个 m-* scoped scss）
- [ ] 2.1 基础件：MButton/MCard/MBlockTitle/MFab/MNavbarBackLink
- [ ] 2.2 列表件：MList/MListItem/MListInput
- [ ] 2.3 表单件：MToggle/MCheckbox/MRange/MSegmented+MSegmentedButton
- [ ] 2.4 浮层件：MDialog+MDialogButton/MSheet/MActions 家族/MPopup/MToast
- [ ] 2.5 导航件：MNavbar（去玻璃→干净表面+hairline）/MTabbar+MTabbarLink（激活 highlight）
- [ ] 2.6 杂项：MPreloader/MCover/MEmpty
- [ ] 验证：每小组 build + lint；MuMu 逐组件确认

### 阶段 3：页面观感对齐（14 页 scoped scss）
- [ ] 3.1 App.vue + MiniPlayer（迷你条玻璃→干净表面/hairline）
- [ ] 3.2 TabsPage（底部导航激活 highlight）
- [ ] 3.3 CategoriesPage（分段条/导航）
- [ ] 3.4 Albums/Artists/Playlists（卡片网格圆角 12dp、行高）
- [ ] 3.5 SettingsPage（列表行高 56px、toggle）
- [ ] 3.6 SongsPage（吸顶随机条玻璃→干净表面）
- [ ] 3.7 LibraryDetailPage/PlaylistDetailPage（虚拟列表行 56px、分割线）
- [ ] 3.8 SourcesPage（卡片列表圆角）
- [ ] 3.9 QueuePage（虚拟队列行）
- [ ] 3.10 PlayerPage（进度条/主控 highlight、编辑弹窗圆角 20dp）
- [ ] 验证：`npm run build` + `npm run lint` 全绿；MuMu 全页面浏览截图

### 阶段 4：全量验收（对照 prd AC1-AC7）
- [ ] 4.1 `npm run build` / `npm run lint` 通过（AC1）
- [ ] 4.2 grep spot-check token 值（AC2）
- [ ] 4.3 组件圆角/色值 spot-check（AC3）
- [ ] 4.4 MuMu 逐页视觉走查（AC4）——无 iOS 蓝/玻璃残留
- [ ] 4.5 播放/队列/歌词/编辑/扫描全流程功能验证（AC5）
- [ ] 4.6 PlayerPage 视觉确认（AC6）
- [ ] 4.7 无 k-* / tailwind / konsta 残留 grep（AC7）

## 验证命令

```bash
npm run build        # vue-tsc + vite build
npm run lint         # eslint .
npx cap sync android # 部署前同步
npx cap run android --target emulator-5556  # MuMu 运行
```

## 回滚点

- 阶段 1 提交可单文件 revert（theme 只动 index.scss）
- 阶段 2/3 每小组/每页独立提交，可按组件/页面粒度 revert
- 全程不动 script / 模板结构 / 组件 API → 逻辑零风险

## 风险清单

- 玻璃类（`.m-glass-*`/`.player-overlay` 等）有页面 DOM 引用 → 先保留类定义，只改常驻条的使用方式；发现漏改则回到阶段 3 对应页
- 深色播放页与 Salt 亮色主页差异大 → 播放页保留深色沉浸（Salt 播放页亦为暗色），分控主题结构预留
- 某组件颜色被页面 `!important` 覆盖 → 逐页检查 scoped 覆盖规则（阶段 3 处理）
