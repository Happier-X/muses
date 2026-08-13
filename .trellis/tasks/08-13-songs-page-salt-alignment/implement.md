# Implement: 歌曲页椒盐视觉深仿

> 状态：规划中，待用户审查设计决策后 `task.py start`。

## 执行顺序（每阶段独立提交，可单独回滚）

### 阶段 1：排序能力（R2）——前置基础
- [ ] 1.1 `views.ts` 新增 `SongSortMode` 类型 + `sortSongsByMode(songs, mode)`（custom/title/fileName/artist/album/duration/folder）
- [ ] 1.2 SongsPage 接入排序状态（sessionStorage 持久 `muses:songs-sort-mode`），`refreshSongs` 按模式排序
- [ ] 1.3 验证：build + 手动切排序列表顺序变化
- 提交：feat(songs): 排序模式与排序菜单数据

### 阶段 2：工具条（R1）
- [ ] 2.1 SongsPage 新增工具条（48dp，surface-1），左侧圆形按钮 + 右侧排序/多选按钮
- [ ] 2.2 排序按钮弹出 MActions 菜单（椒盐 13 项，不可用项置灰），选中项打勾
- [ ] 2.3 验证：MuMu 工具条视觉对齐 + 排序菜单交互
- 提交：feat(songs): 工具条与排序菜单

### 阶段 3：多选（R3）
- [ ] 3.1 多选状态（selectedIds Set + isMultiSelect）+ 行前选择框（勾选样式）
- [ ] 3.2 顶部「已选中 N 项」+ 全选切换；底部操作条（永久删除/添加到歌单/播放选中队列）
- [ ] 3.3 播放选中队列复用 controller（clearQueue+enqueueSongs+playSong）；添加到歌单复用现有 MActions；永久删除复用库移除
- [ ] 3.4 验证：MuMu 全流程
- 提交：feat(songs): 多选模式

### 阶段 4：字母索引条（R4）
- [ ] 4.1 索引分组函数（标题首字符 → 字母/#）+ 有歌字母集合计算
- [ ] 4.2 索引条 UI（16dp 宽、A-Z+#+顶部0、12px 灰字、15dp 间距），仅字母序排序显示
- [ ] 4.3 点击/拖动跳转（scrollToIndex 映射分组首行）
- [ ] 4.4 验证：MuMu 拖动/点击跳转、无歌字母处理
- 提交：feat(songs): 字母索引条

### 阶段 5：行内视觉（R5）
- [ ] 5.1 行高 56→72px（estimateSize + stub 同步）、封面 50dp（含无封面占位）
- [ ] 5.2 行内圆形加队列按钮（42px 圆 + 加号），点击 enqueueSong + toast
- [ ] 5.3 菜单按钮内容对齐椒盐（下一首播放/分享/歌曲信息/永久删除等）
- [ ] 5.4 正在播放行高亮移除 → 行内播放指示（按决策）
- [ ] 5.5 验证：MuMu 行视觉对齐椒盐截图
- 提交：feat(songs): 行内布局对齐

### 阶段 6：搜索（R6）
- [ ] 6.1 navbar 右侧搜索按钮 + 搜索栏（替换工具条区域，autofocus）
- [ ] 6.2 title/artist/album 包含过滤（computed）+ 虚拟列表联动
- [ ] 6.3 取消/清空恢复；placeholder「在 N 首歌曲中搜索」
- [ ] 6.4 验证：MuMu 搜索过滤即时性
- 提交：feat(songs): 搜索

### 阶段 7：全量验收（对照 AC1-AC8）
- [ ] 7.1 `npm run build` / `npm run lint` 全绿
- [ ] 7.2 MuMu 逐项走查（工具条/排序/多选/索引/行视觉/搜索）
- [ ] 7.3 回归：播放/队列/编辑/扫描/滚动位置保存/跳转气泡
- [ ] 7.4 截图对比椒盐（salt-songs-page.png 等已存 .tmp/）

## 验证命令

```bash
npm run build        # vue-tsc + vite build
npm run lint         # eslint .
npx cap sync android # 部署前同步
npx cap run android --target emulator-5556  # MuMu 运行
adb exec-out screencap -p > .tmp/check.png  # 截图对比
```

## 参考素材（已存）

- 椒盐歌曲页截图：`.tmp/salt-songs-page.png`、`.tmp/salt-songs-bottom.png`、`.tmp/salt-one-song.png`、`.tmp/salt-now.png`
- SaltUI 源码：`/tmp/saltui/`（Item.kt / SaltDimens.kt / SaltColors.kt / RoundedColumn.kt）
- APK 反编译：`/tmp/saltplayer-apk/`（cfr-out1/3 混淆代码，参考价值有限）

## 风险与回滚点

- 行高/滚动偏移：虚拟列表 estimateSize 与 stub 同步改，防错位
- 工具条 sticky：复用 shuffle-bar 吸顶经验（top calc + 安全区）
- 索引条仅字母序排序显示——避免 custom 排序下分组错乱
- 每阶段独立提交可回滚；核心文件仅 SongsPage.vue + views.ts
- 若 4.2 索引条视觉与椒盐偏差大，回退阶段 4 单独评估

## 待用户确认的决策（§8 in design.md）

1. 中文歌索引归属：#（推荐）
2. 无歌字母：统一灰全渲染（推荐）
3. 正在播放行指示：行内播放图标（推荐）
4. 排序不可用项：置灰（推荐）
5. 永久删除：仅移除库记录（推荐）
6. 歌曲信息：省略（推荐）
