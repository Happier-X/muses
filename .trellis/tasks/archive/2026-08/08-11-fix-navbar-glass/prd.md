# PRD：Konsta navbar 玻璃效果修复

## 背景

用户反馈："项目的 navbar 和 konstaui.com/vue/navbar 官网不一样，官网是那种玻璃效果"。经 CDP 实测，navbar 玻璃层在模拟器（WebView 110）上**实际未渲染**，两层背景元素计算样式均失效。

## 根因（两个独立问题）

### 1. 背景渐变整层消失（主因）
Konsta k-navbar 的 bg 层 = `bg-gradient-to-b from-ios-light-surface to-transparent`。Tailwind v4 为其生成 `--tw-gradient-position: to bottom in oklab`——`in oklab` 插值语法需 Chromium 111+，**WebView 110 不解析** → `linear-gradient(var(--tw-gradient-stops))` 整体无效 → computed `background-image: none` → navbar 完全透明（只剩标题文字浮在页面上）。

### 2. 毛玻璃模糊层缺失
Konsta bgBlur 层 = `backdrop-blur-[2px]` + `mask-b-from-50%` + `mask-b-to-100%`。实测 build 产物 CSS 中 mask-* 类正常生成，**唯一漏掉含方括号的 `backdrop-blur-[2px]` 任意值类** → `backdrop-filter: none`。mask 类生效但 blur 失效。

> 注：Tailwind 后续构建中该任意值类实际可生成（原产物缺失疑为扫描缓存/版本浮动），修复采用的通用 CSS 规则同时覆盖两种情形。

## 修复内容

`src/theme/tailwind.css` 末尾追加（commit f5765ae）：

```css
.backdrop-blur-\[2px\] {
  --tw-backdrop-blur: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  backdrop-filter: blur(2px);
}

.k-navbar > .bg-gradient-to-b {
  --tw-gradient-position: to bottom;
}
```

- `--tw-gradient-position: to bottom`：去 oklab 插值（同 tabbar 先例 `[--tw-gradient-position:to_top]`）
- backdrop-blur-[2px]：补齐 blur（含 `-webkit-` 前缀，老 WebView 必需）

## 验收（全部通过）

- [x] bg 层 computed background-image = `linear-gradient(rgb(239,239,244) 0%, rgba(0,0,0,0) 100%)`（顶部实灰→渐隐）
- [x] blur 层 computed backdrop-filter = `blur(2px)`；mask 遮罩生效（上半实色、下半渐隐）
- [x] 与官网 demo 视觉一致：顶部浅灰实色、向下渐隐、滚动内容在底部被轻微模糊透出
- [x] 构建通过（vue-tsc + vite）；Konsta toolbar（tabbar）的 Backdrop blur 同享修复
- [x] 附验证截图 navbar-glass-fixed.png（已删除，未入库）

## 备注

- 全项目其他渐变仅 tabbar 一处（已提前修复 to_top），dark 分支 `dark:from-ios-dark-surface/50` 同样跟随 `--tw-gradient-position` 覆盖生效
- 此任务为补记留档：修复与验证在任务创建前已完成