# 设计：跳转当前遮挡 + 播放主控 fill 图标

## 范围边界

| 区域 | 改 | 不改 |
|------|----|------|
| SongsPage FAB 跳转 | 滚动对齐，避开粘性顶栏 | 其它页跳转、虚拟列表 |
| ion-lucide 适配层 | 主控 play/pause/skip 提供 fill | 全局图标语义扫库（#44） |
| MiniPlayer / PlayerPage 主控 | 使用 fill 图标 | 按钮布局、solid 圆底、模式键 |

## #43 滚动遮挡

### 根因

`scrollIntoView({ block: 'start' })` 把目标行对齐到**滚动端口**顶部，不扣除：

1. 外层粘性 `ion-header`（标题 `ion-toolbar` + `.shuffle-toolbar`）
2. 内容内 `ion-header[collapse=condense]` 大标题在部分滚动态的占用

因此行的上半部分被顶栏盖住。

### 方案选择

**首选：`scroll-margin-top`（CSS）**

- 在 `.song-item`（或带 `data-song-id` 的行）设置 `scroll-margin-top`，值覆盖粘性头总高度。
- 优点：继续用现有 `scrollIntoView({ block: 'start' })`，单测断言可不变；与浏览器原生语义一致。
- 高度取值：
  - 经验常量：主 toolbar ~56px + shuffle toolbar ~48px ≈ **104–120px**，可再加少量缓冲（如 8–16px）。
  - 宽窄屏结构相同（均有双 toolbar），优先统一常量；若实测 condense 大标题额外遮挡，可略加大。
- 风险：header 高度随字体/平台变化时常量可能偏差 → 验收时用真机/模拟器 spot-check；偏差大再改为测量。

**备选：IonContent 偏移滚动**

- `content.getScrollElement()` + `element.offsetTop - headerHeight`。
- 更精确，但依赖 Ionic API、异步、单测要改 mock；本任务优先 CSS 方案。

### 数据流 / 调用链

```
FAB click → scrollToCurrentSong()
  → findSongRow(currentId)  // [data-song-id]
  → row.scrollIntoView({ smooth, start, nearest })
  → 浏览器应用 scroll-margin-top → 视觉上完整露出
  → jump-highlight 1.2s
```

## #45 fill 图标

### 根因

`lucideToIonIcon` 固定：

```ts
fill: 'none', stroke: 'currentColor', 'stroke-width': 2, ...
```

Lucide 的 `Play` / `Pause` / `SkipBack` / `SkipForward` 本身是 path/rect 几何，**无独立 Fill 导出名**；实心效果需在适配层改根属性（并对 path 填色）。

### 方案

在 `ion-lucide.ts` 增加可选根属性覆盖，例如：

```ts
lucideToIonIcon(iconNode, { variant: 'fill' | 'outline' })
```

- **outline（默认）**：现状不变。
- **fill**：`fill: 'currentColor'`，`stroke: 'none'`（或 stroke 同色且 width 0），子节点继承/使用 currentColor 填充。

导出策略（推荐，改动面小）：

| 导出名 | 用途 |
|--------|------|
| `play` / `pause` / `playSkipBack` / `playSkipForward` | **改为 fill**（主控当前唯一用法即 MiniPlayer + PlayerPage） |
| 若其它处误依赖 outline 主控语义 | 新增 `playOutline` 等保持 outline（现已有 `playOutline = play`，需拆开） |

**注意**：当前 `playOutline = play`。PlaylistDetail 等使用 `playOutline` 作「播放全部」。  
验收 #45 只强制 MiniPlayer/PlayerPage 主控 fill；「播放全部」可：

- A：一并变为 fill（视觉更统一，可接受）
- B：`play` 变 fill，`playOutline` 保持 outline（语义更干净）

**决策：B** — `play`/`pause`/`playSkipBack`/`playSkipForward` 主控 fill；`playOutline` 继续 outline 给列表/次级「播放」入口。歌词页 `playCircle`/`pauseCircle` 保持 outline（Out of Scope）。

### 兼容

- 业务 import 名不变时，仅主控符号变 fill 即可。
- CSS 字号/颜色不变。
- 无原生通知栏图标依赖此 TS 适配层。

## 规范同步

`component-guidelines.md`：

1. FAB：写明 `block: 'start'` **且** 行需 `scroll-margin-top`（或等价偏移）避开粘性双 toolbar。
2. 图标：播放主控（底栏/沉浸主三键）使用 fill 版 Lucide 适配导出；模式键/次级操作仍 outline。

## 风险与回滚

| 风险 | 缓解 |
|------|------|
| scroll-margin 过大导致上方空白过多 | 用接近 header 实测高度；末尾项仍靠边界 |
| fill path 个别图标发虚/裁切 | 真机看 Play/Pause/Skip；必要时 fill+细 stroke |
| playOutline 与 play 拆分遗漏引用 | rg 全库 `playOutline`/`from '@/icons/ion-lucide'` |

回滚：还原 `SongsPage` 样式/滚动与 `ion-lucide` 导出即可。

## 测试

- 单测：跳转仍断言 `scrollIntoView(..., block: 'start')`（CSS margin 不改调用）。
- 可选：断言 fill 导出 SVG 含 `fill='currentColor'` 且无 `fill='none'`（小单测即可）。
- 手工：SongsPage 中部/顶部/底部当前曲 FAB；MiniPlayer 与 PlayerPage 主控图标实心。
