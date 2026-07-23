---
name: Muses
description: Android 自有曲库播放器——列表克制、沉浸页把舞台留给音乐
colors:
  primary: "#006FEE"
  primary-50: "#E6F1FE"
  primary-100: "#CCE3FD"
  primary-200: "#99C7FB"
  primary-300: "#66AAF9"
  primary-400: "#338EF7"
  primary-500: "#006FEE"
  primary-600: "#005BC4"
  primary-700: "#004493"
  primary-800: "#002E62"
  primary-900: "#001731"
  ink: "#000000"
  ink-muted: "#92949c"
  surface: "#ffffff"
  surface-elevated-dark: "#1f1f1f"
  surface-dark-md: "#121212"
  border-subtle: "#e0e0e0"
  tab-inactive: "#595959"
  immersive-void: "#05070d"
  immersive-ink: "#ffffff"
  immersive-ink-soft: "#adadad"
  immersive-track: "#ffffff33"
  cover-placeholder-mid: "#9478ff"
  playing-tint: "#006FEE1A"
  jump-highlight: "#006FEE38"
typography:
  body:
    fontFamily: "var(--ion-font-family), system-ui, -apple-system, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.4
  title:
    fontFamily: "var(--ion-font-family), system-ui, -apple-system, sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.25
  label:
    fontFamily: "var(--ion-font-family), system-ui, -apple-system, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.2
  immersive-title:
    fontFamily: "var(--ion-font-family), system-ui, -apple-system, sans-serif"
    fontSize: "clamp(20px, 5.6vw, 28px)"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.01em"
  lyric:
    fontFamily: "var(--ion-font-family), system-ui, -apple-system, sans-serif"
    fontSize: "clamp(22px, 6.5vw, 32px)"
    fontWeight: 400
    lineHeight: 1.3
rounded:
  cover-list: "10px"
  cover-list-sm: "8px"
  cover-immersive: "clamp(18px, 4vw, 28px)"
  pill: "999px"
spacing:
  xs: "2px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  tab-bar: "64px"
  mini-player: "64px"
  sidebar: "240px"
  content-max: "720px"
  breakpoint-tablet: "768px"
components:
  mini-player:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.cover-list}"
    height: "64px"
    padding: "8px 12px"
  tab-active:
    textColor: "{colors.primary}"
  song-row-playing:
    backgroundColor: "{colors.playing-tint}"
  progress-bar:
    backgroundColor: "{colors.immersive-track}"
    textColor: "{colors.immersive-ink}"
    height: "4px"
    rounded: "{rounded.pill}"
  lyric-fab:
    backgroundColor: "#00000029"
    textColor: "#c7c7c7"
    rounded: "{rounded.pill}"
    size: "40px"
---

# Design System: Muses

## 1. Overview

**Creative North Star: 「暗场听席」**

列表与导航像关掉场灯的过道：安静、可读、可预期；真正开灯的只有沉浸播放页——封面、动态背景与逐词歌词占满注意力。产品个性是沉静、可靠、专注：工具感来自可信，而不是装饰。

视觉系统建立在 **Ionic 应用壳 + HeroUI primary + 语义 token** 之上。权威 token 与通用组件来自 npm **`happier-ui@0.0.1`**（`happier-ui/tokens.css`，前缀 **`--h-*`**）；`--muses-*` 为兼容别名。默认不得提交 `file:` 依赖或相邻源码 alias。`src/theme/tokens.css` re-import 包 token，`variables.css` 把 `--h-*` 桥接到 Ionic。主色 HeroUI `common.blue`（`#006FEE`）。`src/components/ui` 仅转出库真实导出与 app-only `MCover`/`MPage`。flat 列表 + 沉浸舞台；非 Material。

**Key Characteristics:**
- 双层体验：曲库列表克制；沉浸页是听歌主舞台
- 默认 flat：无顶栏阴影、无 MD elevation 套路
- 主色 = HeroUI primary `#006FEE`（及 50–900 色阶），仅作选中/播放/主操作
- 动效短、服务状态：约 180–220ms，无编排入场
- 平板 ≥768px：侧栏 240px + 内容最大宽 720px 居中

## 2. Colors

色板以 **HeroUI primary**、Ionic 中性语义色与沉浸页硬编码为主；主色已定稿为 HeroUI 默认蓝，仍须克制使用，不得大面积铺陈。

### Primary
- **HeroUI Primary（blue 500）** (`#006FEE` / rgb `0, 111, 238`)：与 HeroUI / NextUI 默认 `primary.DEFAULT` 一致。用于 Tab 选中、列表「正在播放」浅底 (`rgba(0,111,238,0.1)`)、跳转高亮 (`0.22`)、Ionic `color="primary"` 控件。深浅色模式均保持同一 DEFAULT（覆盖 Ionic `dark.system` 的 `#4d8dff`）。
- **色阶（照抄 HeroUI `common.blue`）**：50 `#E6F1FE` · 100 `#CCE3FD` · 200 `#99C7FB` · 300 `#66AAF9` · 400 `#338EF7` · 500 `#006FEE` · 600 `#005BC4` · 700 `#004493` · 800 `#002E62` · 900 `#001731`。Ionic shade/tint 映射为 600 / 400。CSS 别名：`--muses-primary-50` … `--muses-primary-900`。

### Neutral
- **纸面白** (`#ffffff`)：浅色模式 MiniPlayer 与默认内容底
- **暗条** (`#1f1f1f`)：深色 MiniPlayer / 工具条近似面
- **MD 深底** (`#121212`)：Ionic `dark.system` 在 md 模式下的页面底（框架默认，非品牌主张）
- **正文墨**（`var(--ion-text-color)`，浅色近黑）：标题与主文案
- **次要灰** (`#92949c` / `var(--ion-color-medium)`)：副标题、空状态、占位图标
- **分隔** (`#e0e0e0` / `var(--ion-color-step-150)`)：Tab 顶边、侧栏右边线
- **Tab 未选** (`#595959` / step-650)：导航静息色

### Immersive（沉浸页专用）
- **暗场虚空** (`#05070d`)：沉浸壳底色
- **舞台白** (`#ffffff`)：歌名、主控激活、进度已播放
- **柔白文** (`rgba(255,255,255,0.68)` ≈ `#adadad` 观感)：艺人、时间
- **轨道半白** (`rgba(255,255,255,0.2)`)：进度未播放轨
- **无封面氛围紫** (`rgba(148, 120, 255, 0.28)` 径向叠加在 `#171b2b → #05070d`)：仅 fallback 背景，不是品牌强调色

### Named Rules
**The Stage Light Rule.** 列表层禁止大面积饱和色块；饱和色与封面氛围只允许出现在沉浸页或「正在播放」等状态提示。

**The HeroUI Primary Rule.** 主色唯一来源是 HeroUI `common.blue`（DEFAULT `#006FEE`）。禁止回退 Ionic 默认 `#0054e9` / 深色 `#4d8dff`，禁止另起一套「Muses 蓝」。主色只服务状态与主操作，不做英雄色铺底。

## 3. Typography

**Display Font:** 无独立展示字体  
**Body Font:** Ionic / 系统 UI 无衬线（`var(--ion-font-family)` → system-ui 栈）  
**Label/Mono Font:** 同族；时间用 `font-variant-numeric: tabular-nums`

**Character:** 单一无衬线贯穿产品界面。列表偏工具密度；沉浸页用字重与字号拉开层次，而不是换字体。

### Hierarchy
- **Immersive title** (700, `clamp(20px, 5.6vw, 28px)`, lh 1.2)：沉浸页歌名
- **Lyric** (默认 AMLL，`--amll-lp-font-size: clamp(22px, 6.5vw, 32px)`)：逐词歌词主舞台
- **Title / list primary** (600, 约 15px, lh 1.25)：MiniPlayer 曲名、列表 `h2`
- **Body secondary** (400, 13px)：艺人、副文案（`ion-color-medium`）
- **Label / tab** (400–600, 12px)：底栏标签；选中时 `font-weight: 600`
- **Meta** (11–12px)：进度时间、缓冲提示

### Named Rules
**The One Family Rule.** 禁止为「更高级」引入展示衬线或装饰字体；产品 UI 只用系统无衬线。

## 4. Elevation

系统 **默认全 flat**：深度靠 1px 分隔线、背景色差与沉浸页画面本身，不用 Material elevation 阴影语言。

`src/theme/variables.css` 显式去掉 `ion-header` 阴影与 MD 伪阴影。MiniPlayer、Tab bar 用 `border-top` / 实色底分层，而不是 drop shadow。

沉浸页的「深度」来自 AMLL 动态背景或不透明度 0.75 的封面渲染 + fallback 径向渐变，**不是**卡片抬升。

### Shadow Vocabulary
- **无项目级 shadow token。** 禁止为了「更有层次」给列表行、MiniPlayer、FAB 加粗 elevation。
- Ionic 默认 FAB 若带阴影，应视为待收敛的框架债，与 PRODUCT 反例一致时优先压掉。

### Named Rules
**The Flat Corridor Rule.** 列表与导航像过道：平、干净、可扫读。若看起来像 2014 Material 卡片墙，阴影与 elevation 已经过多。

## 5. Components

气质总则：**沉浸页柔和，列表干脆。**

通用语义组件来自 `happier-ui@0.0.1`；`src/components/ui/` 只 re-export 真实库导出，并保留 app-only `MCover`/`MPage`。视觉值走 `--h-*` / `--muses-*` 别名；禁止 `MIon*`、Material elevation，以及在 Muses 新造通用平行 M* 组件。库没有的列表行、设置行、icon-only 按钮等继续保留 Ionic/业务实现并登记任务 `gaps.md`，未来回 happier-ui 开发。

| 组件 | 用途 |
|------|------|
| `HButton` / `HSwitch` / `HInput` / `HCheckbox` | 通用操作与表单控件 |
| `HEmpty` / `HImage` / `HIcon` | 空态、通用图片、`@lucide/vue` 图标渲染 |
| `HTabBar` / `HNavBar` | 导航视觉层；Ionic 路由宿主继续保留 |
| `HBottomSheet` / `HDialog` | 库真实导出；本轮叠层引擎仍保留 Ionic |
| `MCover` | app-only，列表与 MiniPlayer 音乐封面/占位 |
| `MPage` | app-only，简单 Ionic 页壳 |

### Buttons
- **Shape:** 带文字操作优先 `HButton`；纯图标触控因库缺口保留 Ionic 按钮壳，但图标必须是 `HIcon`，并登记 `HIconButton` 缺口；沉浸主控 52×52（播放 68×68），无填充圆钮默认
- **Primary actions:** 依赖 Ionic `color="primary"` 或图标实心 fill（播放主控）
- **Immersive mode buttons:** 默认 `rgba(255,255,255,0.58)`，激活纯白
- **Lyric FAB:** 40×40 胶囊（`border-radius: 999px`），半透明黑底 + 可选 `backdrop-filter: blur(10px)`，仅歌词页按需显示；激活时白底 22% 透明

### List rows / covers
- **Song row（当前 Ionic/业务实现）:** `--min-height: var(--muses-song-row-height)`（72px）；封面 52×52，`border-radius: 10px`；待 happier-ui 提供 `HListRow` 后再回迁
- **Playlist / smaller covers:** 常为 `8px` 圆角（`coverRadius="sm"`）
- **Playing state:** 背景 `rgba(primary, 0.1)`；跳转高亮 `0.22`
- **Placeholder cover:** `rgba(medium-rgb, 0.16)` 底 + medium 图标

### MiniPlayer
- **Height:** 64px；底边贴 Tab 上沿（`bottom: calc(64px + safe-area)`），平板无 Tab 时贴底安全区
- **Surface:** 浅 `#ffffff` + `border-top: rgba(0,0,0,0.08)`；深 `#1f1f1f` + 白 12% 顶边
- **Cover:** 48×48，`radius 10px`
- **Type:** 曲名 15px / 艺人 13px medium
- **z-index:** 1000（在 Tab 30 之上、沉浸 1100 之下）

### Navigation
- **Mobile:** 固定底栏 6 等分，顶部分隔线，项高 ≥52px，图标 22px + 12px 标签
- **Active:** `primary` + `font-weight: 600`
- **Tablet ≥768px:** 左侧 240px 边栏 + 内容区 `left: 240px`；底栏隐藏
- **Content max width:** `--muses-content-max-width: 720px` 居中

### Progress
- **ion-range 定制：** knob 隐藏（`--knob-size: 0`）；轨高 4px；已播放白、未播放 20% 白；全圆角 pill
- **Hit area:** 高度 24px 保触摸

### Immersive player (signature)
- **Shell:** 全屏 `#05070d`，`z-index: 1100`；开合 `transform` + `220ms ease`
- **Cover:** 正方形 `min(72vw, 100%, 340px, 52dvh)`，`radius clamp(18px, 4vw, 28px)`
- **Panels:** 信息 / 歌词横向 50%+50%，滑动 `220ms ease`
- **Safe area:** 上下 padding 计入 `ion-safe-area-*`

### Inputs / Fields
- 设置与音源页沿用 Ionic item / toggle / input 默认形貌；无独立品牌输入皮肤。新表单保持 Ionic 控件，不发明奇异控件。

## 6. Do's and Don'ts

### Do:
- **Do** 把沉浸页当主舞台：封面、歌词、主控优先；列表保持可扫读密度。
- **Do** 用 1px 分隔与背景差分层；顶栏保持无阴影（见 `variables.css`）。
- **Do** 为 Tab + MiniPlayer 预留底部滚动空间（约 128px + safe-area；平板相应减少）。
- **Do** 动效只表达状态（开合沉浸页、队列滑入、FAB 显隐），时长约 180–220ms，`ease` / 非弹性。
- **Do** 尊重安全区、系统返回与「减少动态效果」；沉浸手势不得破坏系统可控性。
- **Do** 图标语义一致（`@lucide/vue` 组件经 `HIcon` 渲染）；播放主控 fill，列表次要动作线框。
- **Do** 触控目标尽量 ≥44–48dp 量级（主控 52–68、Tab ≥52）。

### Don't:
- **Don't** 使用 Material Design / 默认 Ionic MD 套路：厚重 elevation、FAB 阴影、系统控件堆叠感——平台是 Android，视觉不跟 MD。（PRODUCT 反例）
- **Don't** 做成花哨流媒体 App：强运营色块、推荐瀑布流、广告式卡片与促销节奏。
- **Don't** 走极客终端风：纯黑底加霓虹强调、信息密度压过听感。
- **Don't** 给列表卡片加侧边色条、渐变字、装饰性玻璃拟态（歌词 FAB 的轻 blur 是按需控件，不是卡片皮肤）。
- **Don't** 把 HeroUI primary（`#006FEE`）当英雄色铺满页面；也不要混用 Ionic 旧默认蓝。
- **Don't** 在列表层复制沉浸页的大封面圆角与暗场渐变。
- **Don't** 用编排式入场动画或超过约 250ms 的拖沓过渡打断听歌流。
