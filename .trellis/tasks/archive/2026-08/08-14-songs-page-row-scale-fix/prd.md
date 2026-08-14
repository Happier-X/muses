# 歌曲页一比一复刻椒盐（基于 SaltUI 源码 + 实测坐标）

## Goal

用户反馈「歌曲页跟椒盐音乐差距还挺大的」，方向为**行内细节尺寸（圆形按钮/更多按钮太小）**，并要求**基于椒盐源码一比一复刻歌曲页**。

复刻依据（已获取）：
1. **SaltUI 组件库源码**（`Moriafly/SaltUI`，Apache-2.0，已克隆到 `.tmp/saltui/`）——组件级权威参数
2. **椒盐翻译资源**（`Moriafly/SaltPlayerSource` 发布仓库的 `strings.xml`）——UI 文案权威来源
3. **模拟器实测**（椒盐 v12.2.0 与 Muses 同机、uiautomator bounds、PIL 像素测量、CDP DOM 实测）——应用层精确坐标

> 注：椒盐完整应用源码（歌曲页应用层布局）未公开（`SaltPlayerSource` 仅为发布/翻译仓库），因此应用层参数以实测坐标为准，组件层参数以 SaltUI 源码为准，两者交叉验证后一致（见下表 ✓ 标注）。

## 源码参数（SaltUI 权威值）

### SaltDimens.kt（`ui2/.../SaltDimens.kt`）

| 参数 | 值 | 说明 |
|------|-----|------|
| item（行最小高度） | **56dp**（Android） | 歌曲页应用层自定义为 72dp（实测 ✓） |
| itemIcon | **24dp**（Android） | 行内图标尺寸 |
| padding | **16dp** | 行内左右 padding |
| subPadding | **12dp** | 行内上下 padding / 元素间距 |
| corner | 12dp | 圆角（已弃用，用 shape） |
| dialogCorner | 20dp | 对话框圆角（已弃用） |

### SaltShapes.android.kt

| 参数 | 值 |
|------|-----|
| small | 8dp |
| medium | 16dp |
| large | 24dp |

### SaltColors.kt（浅色 defaultLight）

| 参数 | 值 | Muses token |
|------|-----|-------------|
| highlight | #0470E6 | --m-primary ✓ |
| text | #1E1715 | --m-text ✓ |
| subText | #8C8C8C | --m-text-2 ✓ |
| background | #F3F3F3 | --m-surface ✓ |
| subBackground | 0x80FFFFFF（80% 白） | --m-surface-1 ✓ |
| stroke | subText 15% 透明 | --m-hairline 系 ✓ |

深色：highlight #0088FF、background #202020、text #EBEEF1、subText #BFE1E6EB、subBackground 3% 白。

### SaltTextStyles.kt

| 参数 | 值 |
|------|-----|
| main | 平台默认（Android ≈ 16sp） |
| sub | **12sp** |
| paragraph | 16sp / lineHeight 1.5em |
| largeTitle | **24sp SemiBold** |

### Padding.kt + Item.kt（行结构）

```
Row(heightIn(56dp), innerPadding[16/12/16/12], CenterVertically)
  ├─ icon 24dp
  ├─ Spacer(subPadding = 12dp)          ← 图标-文字间距
  ├─ JustifiedRow(spaceBetween = subPadding)
  │    ├─ Column: Text(title, main 样式)
  │    │         Spacer(2dp)            ← 标题-副文字间距 2dp
  │    │         Text(sub, sub 样式 12sp, subText 色)
  │    └─ tag（右侧）
  └─ Spacer(subPadding = 12dp)          ← 文字-尾部间距
```

### 翻译文案（strings.xml，歌曲页相关，已与 Muses 对齐 ✓）

- 工具条：歌曲 / 排序 / 搜索 / 多选
- 排序菜单 13 项：自定义、标题、专辑（音轨）、大小、文件夹（标题）、文件名、艺术家（专辑）、年份、播放次数、时长（短→长）、时长（长→短）、修改时间、添加时间
- 多选：全选、取消全选、多选、播放队列（%d）、添加到歌单、永久删除 %s 吗、移除这首歌曲
- 搜索占位：在 %d 首歌曲中搜索
- 行内操作：加入队列（toast）、更多（PopupMenu：下一首播放/添加到歌单/歌曲信息/移除）

## 实测参数（应用层，模拟器 1080x1920 @3x）

### Muses 现状（CDP DOM 实测）

| 项 | 现状 | 差距 |
|----|------|------|
| body margin | **8px（未重置，全局 bug）** | 页面右移 8px、窄 16px |
| 行高 | 72px | ✓ |
| 封面 | 50px（x24 起） | 需左移 8px（修 body margin 后 x16） |
| 圆形按钮 | 14x14px 即交互区 | **交互区需扩至 44x48dp** |
| ⋮ 按钮 | 32x32px 细线图标 | **需扩至 36x48dp + 实心点** |
| 行内横布局 | 封面 x24-74 → 文字 → 圆 x302-316 → ⋮ x320-352 | 圆/⋮ 位置偏右 |

### 椒盐歌曲页（实测）

| 项 | 值 |
|----|-----|
| 行高 | 72dp |
| 封面 | 50dp（x16 起） |
| 圆按钮视觉 | 14dp 圆（底 #EDEDED、图标蓝灰 (148,159,171)） |
| 圆按钮交互区 | **44x48dp**（x264-308） |
| ⋮ 交互区 | **36x48dp**（x308-344，紧挨圆按钮） |
| ⋮ 视觉 | 三点总高 14dp、实心点 ~3.3dp |
| 标题/副文字 | 16sp / 12sp，间距 2dp，副文字「歌手 - 专辑」 |

## 差距与修复（In Scope）

- **D1 全局 body margin**：`src/theme/index.scss` body 加 `margin: 0`（全局 bug，所有页面受益）
- **D2 圆形按钮**：round-btn 交互区 14→**44x48px**（椒盐实测），视觉圆 14px 居中（#ECECEC），图标加粗（stroke-width 3）+ 颜色 #949FAB（椒盐蓝灰）
- **D3 更多按钮**：more-btn 32x32→**36x48px**，lucide 细线改**实心三点**（点 3.5px、gap 2px、总高 ~14px，对齐椒盐视觉）
- **D4 行内横布局**：修 D1 后封面 x16dp；圆交互区 x264-308、⋮ x308-344 紧挨（gap 0）；文字区宽度与椒盐一致（~198dp）
- **D5 行内文字细节**：标题-副文字间距 1→**2px**（SaltUI Item 源码 2dp）；副文字「歌手 - 专辑」已对齐

## Out of Scope

- ❌ 完整应用源码不可得的部分（工具条动画、正在播放视图、PopupMenu 内部结构）——以实测坐标 + 现有交互近似复刻
- ❌ 排序/多选/索引条/搜索逻辑（已完成且未反馈问题）
- ❌ 行高/封面/字号色值（已对齐）

## Acceptance Criteria

- [ ] AC1：`npm run build` / `npm run lint` 全绿
- [ ] AC2：CDP 实测 body margin 0、#app x=0 w=360（满屏）
- [ ] AC3：CDP 实测 round-btn 44x48、视觉圆 14px 居中；more-btn 36x48、实心三点总高 ~14px
- [ ] AC4：截图对比行 1：封面 x16、圆交互区 x264-308、⋮ x308-344（对齐椒盐实测）
- [ ] AC5：标题-副文字间距 2px；副文字 12sp 色 --m-text-2
- [ ] AC6：圆/⋮ 点按功能零回归（加队列 toast / 更多菜单）
- [ ] AC7：其他页面与 MiniPlayer 无回归（body margin 修复影响面验证）
