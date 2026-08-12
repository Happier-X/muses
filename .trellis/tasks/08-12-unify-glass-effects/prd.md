# PRD：玻璃效果统一（随机播放条 + MiniPlayer → Konsta 官方灰玻璃）

## 背景

用户要求全 app 玻璃效果统一，**包括底部播放条**。经官方源码（konsta 5.3.0 `GlassClasses.js` / `NavbarClasses.js` / `ToolbarClasses.js`）与官方文档（konstaui.com/vue/glass）确认：

| 组件 | 配方 | 结构 |
|---|---|---|
| k-glass | 白 0.75 + blur-lg(16px) + 内阴影 | 单层 |
| k-navbar / k-toolbar | 灰渐变 `from-ios-light-surface` + blur-[2px] + mask 渐显 | 双层（blur 层 + bg 层） |

即 k-glass（白玻璃，iOS 26 liquid glass）与系统栏（灰玻璃，iOS 7+ frosted）是**两套不同配方**。

## 方案（已定）

**全 app 统一为 Konsta 官方系统玻璃配方**（灰渐变 + blur2px + mask 渐显双层结构）：

- **navbar / tabbar**：官方组件，不动
- **随机播放吸顶条**（SongsPage + LibraryDetailPage）：k-glass 单层 → 双层（blur 层 `backdrop-blur-[2px]` + `mask-b-from-50%/to-100%`，无背景；bg 层 `bg-gradient-to-b from-ios-light-surface to-transparent`，无 blur 无 mask）
- **MiniPlayer**（悬浮胶囊）：k-glass → 双层胶囊（blur 层 rounded-full + bg 渐变层 rounded-full；**无 mask**——胶囊悬浮不贴边，渐显不适用；渐变提供层次）

## 关键设计约束

1. **双层必须分离**（教训 08-12）：mask 不能与 blur/渐变叠在同一元素，否则 mask 会同时裁掉 blur 和渐变的下半 → 无玻璃感
2. 装饰层 `absolute inset-0` + `pointer-events-none`，内容层 `relative`（保持可点击）
3. 胶囊圆角：blur 层与 bg 层都要 `rounded-full`（否则圆角区域透底）
4. 复用已有手写工具类（tailwind.css）：`.backdrop-blur-\[2px\]`、`.mask-b-from-50\%`/`.mask-b-to-100\%`、`.k-navbar > .bg-gradient-to-b`（渐变方向）——**新增 `.mini-player-glass > .bg-gradient-to-b` 渐变方向覆盖**（WebView<111 兼容，同 navbar 先例）

## 改动范围

- `src/views/SongsPage.vue`：shuffle 条 k-glass → 双层
- `src/views/LibraryDetailPage.vue`：同上
- `src/components/MiniPlayer.vue`：k-glass → 双层胶囊（保留 fixed/rounded-full/事件/aria；`kGlass` import 移除）
- `src/theme/tailwind.css`：渐变方向覆盖选择器补充 `.mini-player-glass`（MiniPlayer 的 bg 层）

## 验收标准

1. 随机播放条与 navbar 视觉一致（灰渐变 + 弱模糊 + 渐显），歌曲滚过有模糊过渡
2. MiniPlayer 胶囊为灰玻璃（与 tabbar 同系），播放/暂停/队列按钮、封面、点击开播放页均正常
3. 浅色/深色模式正常（`dark:from-ios-dark-surface/50`）
4. 构建通过；Edge 真实页面像素验证（玻璃区方差 vs 列表区）；模拟器 computed style 确认
