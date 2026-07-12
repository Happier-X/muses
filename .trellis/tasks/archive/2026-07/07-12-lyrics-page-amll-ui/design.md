# 歌词页 AMLL 视觉复刻 — 技术设计

## 范围

| 在范围内 | 不在范围内 |
|---------|-----------|
| `PlayerPage.vue` 歌词 panel 布局与样式 | 播放引擎 / 队列 / 媒体会话 |
| AMLL `LyricPlayer` 参数与 CSS 定制 | 替换 AMLL / 自研滚动 |
| 窄屏顶部歌名+歌手条 | 歌词页底部迷你控制 |
| 宽屏右侧仅歌词（无顶部信息） | 控制页主布局重排 |
| 单测断言更新 | 新歌词源 / TTML |

## 现状

- 歌词 panel 仅挂载：
  ```vue
  <LyricPlayer class="lyric-player" :lyric-lines="lyricLines" :current-time="..." />
  ```
- 未传 `alignAnchor` / `alignPosition` 等，默认垂直居中。
- 无歌词页专属顶部信息区；歌名/歌手只在控制页。
- CSS 仅保证 `.lyric-player` 占满 panel 高度。
- AMLL 背景仍由整页 `.amll-background` 提供，歌词 panel 不单独换背景。

## 布局方案

### 窄屏 `.lyric-panel`

```
┌─────────────────────────┐
│ safe-area + padding     │
│ 歌名 (h2, 左对齐, 截断)  │
│ 歌手 (p, 次要色, 截断)   │  ← 仅窄屏；无歌手则不渲染副标题
├─────────────────────────┤
│                         │
│   AMLL LyricPlayer      │  ← flex:1; min-height:0
│   左对齐大字            │
│   当前行 ~35%–45% 高度  │
│                         │
├─────────────────────────┤
│ bottom safe-area only   │
└─────────────────────────┘
```

结构建议：

```vue
<section class="panel lyric-panel">
  <header v-if="playerState.currentSong" class="lyric-header">
    <h2 class="lyric-title">{{ title }}</h2>
    <p v-if="artist" class="lyric-artist">{{ artist }}</p>
  </header>
  <template v-if="hasLyrics">
    <LyricPlayer
      class="lyric-player"
      :lyric-lines="lyricLines"
      :current-time="..."
      align-anchor="center"
      :align-position="0.38"
      :enable-spring="true"
      :enable-blur="true"
      :enable-scale="true"
      :word-fade-width="0.5"
    />
  </template>
  <div v-else class="lyric-empty">...</div>
</section>
```

宽屏：`.lyric-header { display: none }`（或 `v-if` 配合媒体查询 class），AMLL 参数不变。

### 为什么顶部信息用 CSS 隐藏而不是拆两套组件

- 单套 DOM 更简单，手势/测试不需分叉。
- 宽屏隐藏 header 即可满足「不重复信息」。

## AMLL 参数选择

| 参数 | 值 | 理由 |
|------|----|------|
| `alignAnchor` | `center` | 当前行自身中心对齐到目标位置 |
| `alignPosition` | `~0.38` | 参考图当前行约在 40% 高度，略偏上 |
| `enableBlur` | `true` | 前后行虚化，贴近参考图 |
| `enableScale` | `true` | 当前行放大 |
| `enableSpring` | `true` | 默认弹簧；若真机卡顿再降级 |
| `wordFadeWidth` | `0.5` | 默认；LRC 行级为主 |

字号/对齐通过 CSS 覆盖 AMLL 内部类（以运行时 DOM / `style.css` 为准），目标：

- 主文字左对齐、偏大（约 `clamp(22px, 6.5vw, 32px)` 量级，实现时按真机微调）
- 行距宽松
- 左右 padding 与顶部信息左缘对齐（约 24px，与 panel padding 协调）

**注意**：不修改 `node_modules`；仅用页面 scoped / `:deep()` 覆盖。

## 数据与文案

- 歌名：`playerState.currentSong.title`
- 歌手：`playerState.currentSong.artist`（空则不渲染副标题；不回退「未知歌手」以免歌词页噪声）
- 不复用控制页 `subtitle`（其含专辑拼接）

## 手势兼容

- 下滑关闭：`canStartVerticalDismiss` 已检查 `scrollTop > 0` 的可滚动祖先；顶部 header 不引入独立滚动容器。
- AMLL 内部滚动容器若 `scrollTop>0` 仍阻止 dismiss——保持现有行为。
- 左右滑：不改 `activePanel` / `translateX(-50%)` 约定。
- 顶部信息区不拦截横向滑动（不新增 `touch-action` 冲突）。

## 测试影响

- 现有「有封面和歌词时…」用例：左滑后仍可见 AMLL 歌词。
- 新增/扩展：
  - 窄屏结构存在 `.lyric-header` 且含歌名；有歌手时含歌手文案
  - 无歌词时仍显示空状态，header 仍可显示歌名
  - mock 的 `LyricPlayer` 可透传/忽略新 props（不必断言 props，除非方便）

## 风险与回滚

| 风险 | 缓解 |
|------|------|
| AMLL 内部 class 变更导致 CSS 失效 | 优先用官方 props；CSS 仅作增强 |
| 大字 + spring 在低端机卡顿 | 可关 `enableSpring` 回退 transition |
| header 挤占歌词区 | header 固定紧凑高度；短屏可略减字号 |
| 宽屏误显示 header | media query 明确 `display: none` |

回滚：还原 `PlayerPage.vue` 歌词 panel 与相关测试即可。

## Spec 回写点

实现后在 `.trellis/spec/frontend/state-management.md` 或 component 规范补充：

- 窄屏歌词页 header：歌名 + 歌手，无底部控制
- 宽屏歌词栏无 header
- AMLL `alignPosition` 中上、左对齐大字约定
