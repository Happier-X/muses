# 界面共用二期技术设计

## 1. T2 抽象（首攻）

| 文件 | 安卓依赖 | 方案 |
|---|---|---|
| SaltShadows | BlurMaskFilter/Paint/nativeCanvas（绘制函数） | 令牌（SaltShadowLayer/Tokens）直接上收；`saltShadow` 拆 expect/actual（安卓保留实现，桌面简化单层/跳过） |
| GlassSurface | `android.os.Build`（import 未实际使用） | 删无用 import 后直接上收；`Modifier.blur` 是跨平台 API |
| MusesHaze | CompositionLocal<HazeState?>（Haze 是跨平台库） | ui-shared 已有 HazeBlurState 桥接，本文件删除，用桥接替代 |

## 2. 播放页（次攻，需用户验收 Haze 降级）

- `PlayerScreen.kt` 约 1242 行：沉浸式歌词（逐词渐变/Blur 距离场/跟手手势/HorizontalPager）。
- 策略：先把非歌词部分（封面/进度/控制栏/模式栏）上收为共用组件；歌词面板视 Haze 降级验收结果定——验收通过则共用，不通过则桌面保留简化版。
- 手势铁律（底层指针处理同一手势内 tap+drag、seek 期间禁 pager 滚动）是纯逻辑，可直接上收。

## 3. 刮削页（末攻，需产品决策）

- 共约 2700 行（ScrapeScreen/Review + 双 ViewModel + 编辑表/队列跟踪）。
- 先定产品决策：桌面端是否上刮削功能。不上则给出书面结论归档；上则按设置页模式（纯 UI + 回调注入）共用化。
