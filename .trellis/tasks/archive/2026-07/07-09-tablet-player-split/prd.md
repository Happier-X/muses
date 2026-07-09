# 平板 Player 页宽屏双栏（封面控制 + 歌词左右并排）

## 目标

在屏幕物理宽度 ≥768px 时，Player 全屏页从当前横向滑动双面板（封面/控制 ↔ 歌词通过手势 `translateX` 切换）改为左右固定双栏同时显示。窄屏保持原有单栏滑动交互无回归。

归属于父任务 `07-09-tablet-adapt` 子任务 C。

## 背景与现状

- `src/views/PlayerPage.vue` 当前使用 `.panels` 容器宽 200%，内含两个 `.panel`（info-panel + lyric-panel），通过 `translateX` 滑动切换
- `activePanel` ref 控制当前面板（0=封面控制，1=歌词），手势 `touchstart/touchend` 横向滑动切换
- 封面 `width: min(72vw, 320px)`；歌词使用 `@applemusic-like-lyrics/vue` 的 LyricPlayer
- 背景使用 `@applemusic-like-lyrics` 的 `BackgroundRender + MeshGradientRenderer`
- 大量 `100vh`/`calc(100vh - 84px)` 等手机比例尺寸（父任务 E 后续校准横屏，本轮不动）

## 关键决策（已与用户确认）

| 决策 | 取值 |
|------|------|
| 宽屏分栏比例 | 左右各 50% 固定双栏 |
| 封面尺寸 | 宽屏自适应 `min(40%, 320px)`，窄屏保持 `min(72vw, 320px)` |
| 分栏切换方式 | 纯 CSS `@media (min-width: 768px)` 加载双栏布局，JS 变量/手势不变 |
| 窄屏回归 | `.panels` + `translateX` + `touchstart/touchend` 全部保留，媒体查询外不生效 |

## 需求

| ID | 需求 |
|----|------|
| R1 | 宽屏 ≥768px 下，Player 页显示左右 50/50 双栏：左栏封面+歌曲信息+进度条+控制按钮+模式按钮；右栏歌词 LyricPlayer（或无歌词时的空状态） |
| R2 | 宽屏下 `activePanel` 变量和手势滑动逻辑保留但不产生视觉切换效果（双栏总是同时显示） |
| R3 | 宽屏下封面尺寸 `min(40%, 320px)`，自适应 |
| R4 | 窄屏 <768px 下布局和交互与改动前完全一致（单栏滑动，无回归） |
| R5 | 宽屏下 `@applemusic-like-lyrics` 背景渲染正常运作（BackgroundRender 覆盖全屏不变乱） |
| R6 | 宽屏下歌词 LyricPlayer 高度自适应（不锁死 `70vh`，填充右栏可用空间） |

## 技术约束

- 只改 `src/views/PlayerPage.vue`
- 不修改 `@applemusic-like-lyrics` 三方组件源码
- 不用新 JS 逻辑，所有双栏切换用 CSS `@media (min-width: 768px)`
- 保留所有现有组件 import 和逻辑变量（`activePanel`、`touchStartX`、`onTouchStart/End` 不动）
- 背景层（`.amll-background` / `.fallback-background`）不随双栏左右分割——继续铺满全屏

## 边界情况

| 场景 | 预期 |
|------|------|
| 宽屏 + 有歌词 | 左栏封面控制，右栏 LyricPlayer 正常播放 |
| 宽屏 + 无歌词 | 左栏封面控制，右栏显示"暂无歌词"空状态 |
| 宽屏 + 无歌曲（empty-state） | 与窄屏一致，全宽居中显示占位 |
| 窄屏 375px→768px 边界 | 媒体查询平滑切换，无闪烁残留 translateX |
| 宽屏仍可触摸面板 | `onTouchStart/End` 未移除，但 CSS 使面板不移动——可保留代码等后续让用户选方向 |
| 暗色模式 | 与现有兼容（所有颜色不变，纯布局改动） |

## 验收标准

- [ ] 浏览器 devtools 宽度 ≥768px：Player 页左右双栏同时可见，左栏封面+控制，右栏歌词/空状态
- [ ] 浏览器 devtools 宽度 <768px：与改动前完全一致（单栏滑动切换）
- [ ] 宽屏封面 `min(40%, 320px)` 自适应，不溢出左栏
- [ ] LyricPlayer 在宽屏右栏内正常显示歌词滚动
- [ ] `npm run build` 通过
- [ ] `npm run lint` 零错误

## 超出范围

- Queue 页双栏改造（留作后续独立判断）
- 横屏 `100vh`/`min-height` 校准（留作父任务 E）
- 手势滑动功能删除（保留代码，仅 CSS 覆盖视觉效果）