# Design: 歌曲页椒盐视觉深仿

## 1. 目标与边界

只改 `src/views/SongsPage.vue`（及其局部样式），复用现有 m-* 组件与 Salt token。**零改动**：播放器/队列/歌单/音源/设置等页面、player/controller 核心逻辑、SongItem 数据结构（不改字段，排序用现有字段派生）。

## 2. 页面结构设计（自上而下，对齐椒盐实测）

```
┌─ MNavbar（现有，navbar-wrap absolute top-0 z-20）────────┐
│  [左: 侧边栏汉堡]  歌曲   [右: 搜索按钮]                 │
├─ 工具条（新，48dp，surface-1 干净表面）──────────────────┤
│  [全选(多选时)/圆形图标]    ...    [排序] [多选]          │
├─ 搜索栏（搜索激活时替换工具条，或独立行）────────────────┤
│  [🔍 在 N 首歌曲中搜索]                    [取消]        │
├─ 列表区（虚拟列表，行高 72dp）──────────────────────────┤
│  [封面50dp] 标题 / 副标题        [○加号] [⋯]            │
├─ 右侧字母索引条（fixed/absolute，x 右 16dp 宽）──────────┤
│  0 A B C ... Z #（可拖动）                              │
├─ 多选底部操作条（多选模式，固定底部 MiniPlayer 上方）─────┤
│  [永久删除] [添加到歌单] [播放选中队列]                   │
└─ MiniPlayer（现有，不动）───────────────────────────────┘
```

关键决策：**工具条与搜索栏互斥**——搜索激活时显示搜索栏（椒盐行为：navbar 右侧点搜索 → 输入框 + 取消），隐藏工具条；取消后恢复。

## 3. 数据与排序设计

### 3.1 排序实现（R2）

`sortSongsForDisplay` 目前只按标题。新增排序模式枚举 + 排序函数（views.ts 或 SongsPage 内部）：

```ts
type SongSortMode = 'custom' | 'title' | 'fileName' | 'artist' | 'album' | 'duration' | 'folder'
```

| 椒盐菜单项 | Muses 映射 | 字段/实现 |
|-----------|-----------|----------|
| 自定义 | custom | 保持当前显示顺序（不排序） |
| 标题 | title | `title.localeCompare(zh-Hans-CN)`（现有 compareText） |
| 文件名 | fileName | `path.split('/').pop()` 比较 |
| 艺术家（专辑） | artist | `getSongArtistName(song)` 比较，次级 album |
| 专辑（音轨） | album | `getSongAlbumName(song)` 比较 |
| 大小 | ❌ | SongItem 无 fileSize 字段（需扩展数据模型，本期不做） |
| 文件夹（标题） | folder | `path` 目录部分比较 |
| 年份/播放次数/修改时间/添加时间 | ❌ | 无数据字段 |

菜单渲染与椒盐一致（13 项，不可用的置灰或隐藏——设计决策见 §8）；选中项打勾。排序模式持久化 sessionStorage（`muses:songs-sort-mode`）。

### 3.2 字母索引（R4）

- **分组键**：取歌曲标题首字符 →
  - `A-Z a-z` → 大写字母
  - `0-9` → `#`（椒盐数字归 # 组）
  - 中文/其他 → **决策点**：无拼音库时按 `localeCompare` 归属 → 中文排在字母后，全部归 `#` 组（在 PRD 已声明不做拼音库，除非必须）
- **索引条渲染**：A-Z + # 全量渲染（含顶部 0 标记），每字母 15dp 高、12px 字、统一灰 `var(--m-text-3)` 色；**有歌字母可点击/拖动跳转，无歌字母点击跳最近有歌组**（椒盐视觉是统一灰不区分，交互上跳最近）
- **可见性**：仅当排序模式为 title/fileName/artist/album/folder（字母序）时显示；custom/duration 等不显示（对齐"仅特定排序下"）
- **跳转实现**：字母 → 计算该组第一个 song 在排序后数组中的 index → `rowVirtualizer.scrollToIndex(index, { align: 'start' })`（现有能力复用）
- **顶部 0 标记**：点击回顶（scrollToIndex 0）

## 4. 工具条与多选（R1/R3）

### 4.1 工具条
- 结构：48dp 高，`position: sticky; top: <navbar 高度>` 或与列表同滚动容器内 sticky（参考现有 shuffle-bar 模式），背景 `var(--m-surface-1)`，无分割线
- 左侧按钮：非多选时圆形图标（椒盐为圆形全选图标——用 `checkCheck` / `circle` lucide 图标近似），点击直接进入多选并全选？——**椒盐行为**：普通模式该按钮点击无弹层（实测），多选模式变「全选」文字。设计：普通模式点击 = 进入多选模式；多选模式点击 = 切换全选
- 右侧排序/多选按钮：clear 变体图标按钮，lucide `arrowUpDown`（排序）+ `checkSquare`/`listChecks`（多选）

### 4.2 多选模式
- 状态：`selectedIds = ref<Set<string>>`，`isMultiSelect = ref(false)`
- 进入：点多选按钮；退出：取消按钮或全选关闭
- 行前选择框：行内 media 左侧加圆形勾选框（椒盐多选行有选择框），点击切换选中
- 顶部工具条变「已选中 N 项」+ 全选
- 底部操作条：固定 bottom（MiniPlayer 上方），三个按钮（永久删除 danger / 添加到歌单 / 播放选中队列）
  - 永久删除：调用现有删除能力（扫描器 remove？需确认——若删除文件需原生能力，本期仅移除库记录+确认弹窗，或标注不支持）
  - 添加到歌单：复用现有 `MActions` 歌单选择弹窗
  - 播放选中队列：`clearQueue + enqueueSongs(selected) + playSong(first)`

## 5. 行内视觉（R5）

复用 MListItem，调整 scoped 样式：
- 行高：`estimateSize` 72（对齐椒盐 72dp），`--m-list-row-h` 覆盖为 72px（scoped）
- media：MCover size 50 radius sm；无封面时 MCover 已有占位逻辑（确认 MCover 无封面渲染——若为纯色块，补圆占位图标）
- 圆形加队列按钮：行内 after 第一个按钮，42px 圆，`--m-surface-2` 底 + `--m-text-2` 加号图标（椒盐实测浅灰圆+深灰加号），点击 `enqueueSong` + toast
- 菜单按钮：现有 `⋯` 保持，菜单内容对齐椒盐：添加到歌单/下一首播放/分享（复制标题）/编辑标签（跳现有编辑流程？椒盐跳外部——Muses 无外部编辑，改为现有编辑入口或省略）/歌曲信息（新 dialog 或省略）/永久删除
- 副标题：椒盐副标题实测为「艺术家 - 专辑」？——APK 实测行内 subtitle 显示 `space x - 0321`（artist - album）。Muses 现有「歌手 - 专辑」一致，保持
- **正在播放行**：移除 10% 主色底（椒盐无），改椒盐式指示——行内加号按钮变播放中图标（`volume-2` / 音符）或标题变主色。**决策点**

## 6. 搜索（R6）

- navbar right 加搜索按钮（lucide `search`），点击 → 工具条区域替换为搜索栏（MListInput 或自定义 input，autofocus）
- 过滤：`computed` 基于 title/artist/album 包含匹配（大小写不敏感），虚拟列表 count 用过滤后数组
- 取消：清空 + 恢复工具条；显示「在 N 首歌曲中搜索」placeholder（N=总数，椒盐实测文案）

## 7. 兼容与风险

- **虚拟列表**：行高改动需同步 `estimateSize: 72` 与退化 stub 的 72；`scrollToCurrentSong` 偏移逻辑检查（scrollToIndex 方案，不受行高影响）
- **滚动位置保存**：SCROLL_SAVE_KEY 机制不变；排序切换时重置滚动位置（避免错位）
- **工具条 sticky 与 navbar 覆盖式**：现有 SongsPage 是覆盖式 navbar（absolute top-0）+ 列表容器从屏幕顶开始。工具条需吸在 navbar 下方（`top: calc(max(16px, var(--m-safe-area-top)) + 44px)`，参考现有 shuffle-bar 吸顶写法）。索引条 fixed 于列表容器内
- **索引条拖动**：touch 事件映射字母（`(clientY - top) / 15dp 高度`），拖动时实时跳转；防抖
- **WebView<111**：无 color-mix/oklab；新颜色一律 var() 或 rgba
- **删除歌曲**：确认现有删除入口（LibraryDetail/PlaylistDetail 有移除按钮）复用其函数；若仅本地库记录无文件删除，则弹确认后从库移除
- **z-index 阶梯**：工具条 20、搜索栏 20、索引条 15（列表内）、多选底部条 25（MiniPlayer 1000 之下，用现有浮层阶梯）

## 8. 待定决策（implement 前定案）

1. **中文歌索引归属**：#（推荐，无拼音库） vs 拼音首字母（需引入依赖）
2. **无歌字母**：统一灰全渲染（椒盐实测如此，推荐） vs 仅渲染有歌字母
3. **正在播放行指示**：行内加号按钮变播放图标（推荐，对齐椒盐圆形按钮位） vs 标题主色
4. **排序菜单不可用项**：置灰显示（推荐，对齐椒盐 13 项菜单） vs 隐藏
5. **永久删除**：仅移除库记录（推荐，安全） vs 真删文件（需原生权限）
6. **歌曲信息/编辑**：省略（推荐，Muses 无外部编辑） vs 做简单信息弹窗

## 9. 回滚点

- 单文件改动为主（SongsPage.vue + views.ts 排序函数 + 可能 MListItem scoped 微调），可整文件 revert
- 新增排序函数独立提交；工具条/索引条/多选/搜索各阶段独立提交可分别回滚
