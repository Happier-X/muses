# PRD：沉浸式播放页顶部标题艺术家固定复用，左右滑动只切换下方内容（椒盐式）

## 目标与用户价值

沉浸式播放页（PlayerPage.vue）在**手机布局（<768px）**下改为椒盐音乐式交互：顶部歌名/艺术家作为**固定头部**常驻，左右滑动切换时只切换下方内容（信息区 ⇄ 歌词区），提升操作连贯性与沉浸感。

## 已确认事实（代码证据）

- 当前 `panels` 容器（宽 200%，`translateX(-activePanel * 50%)` 切换，0.22s easeOut）内含两个面板：
  - **info-panel**：`player-page__song-head`（标题+艺术家，左对齐）+ 大封面 + 五行歌词窗口 + 进度条 + 控制按钮 + 模式栏；`info-inner` 为 `justify-content: flex-end`（内容底部对齐），`padding-top: 16px` 为头部占位
  - **lyric-panel**：`lyric-header`（标题+艺术家）+ AMLL `LyricPlayer`（flex:1）；另有底部浮动 chrome（翻译键/播放键）与点击显示逻辑
- 手势逻辑（`onTouchStart/Move/End`）：横向滑动切面板（`activePanel = endX < startX ? 1 : 0`），纵向下滑关闭；`seekGestureLocked` / `isNativeInteractiveEvent` 保护进度条与按钮；歌词面板内滑动禁止下滑关闭
- 平板（≥768px）：全局 index.scss 断点让 `panels` 收为 width:100%，两面板左右并排（`flex: 1`），左右滑动关闭（transform:none）；info-panel 居中 padding 24px；两个面板各自显示自己的标题/艺术家头部
- 空态（无当前歌曲）：`empty-state` 位于 panels 之外独立渲染，无头部
- 歌词翻译显隐、歌词点击 seek、切歌保持面板等逻辑与头部无耦合

## 需求

1. **固定头部（手机 <768px）**：歌名/艺术家渲染在 panels 容器之外（drag-layer 内），左右滑动时头部不移动；视觉样式保持现状（左对齐，标题 20px / 艺术家 13px）
2. **复用（手机）**：歌词面板隐藏自身的 `lyric-header`，两个面板共用固定头部
3. **滑动区只含下部内容（手机）**：info-panel 隐藏 `song-head`，保留 封面 + 五行歌词窗口 + 进度条 + 控制按钮 + 模式栏；lyric-panel 保留 AMLL 全屏歌词 + 底部浮动 chrome
4. **平板（≥768px）保持现状不动**：两个面板仍各自显示自己的头部，左右分栏不变；本次不调整平板布局（后期另做）
5. 手势/关闭/seek/歌词点击等现有交互行为不变

## 验收标准

- A1 手机：左右滑动切面板时，顶部歌名/艺术家位置完全不动
- A2 手机：歌词面板不重复显示标题/艺术家
- A3 手机：信息面板与歌词面板的滑动切换、下滑关闭、进度条拖动、歌词点击 seek、歌词浮动 chrome 行为与改动前一致
- A4 平板（≥768px）：视觉与改动前完全一致（左右分栏，各面板自带头部）
- A5 空态（无歌曲）不受影响

## 范围外

- 不改动平板布局（后期单独设计）
- 不改动 MiniPlayer、队列页、编辑歌曲信息弹窗
- 不引入新的滑动/手势库
- 不做「头部随滑动淡出/滚动」等附加动画
