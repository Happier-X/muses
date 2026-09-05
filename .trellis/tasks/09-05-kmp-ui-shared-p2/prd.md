# KMP界面共用二期-播放页与刮削页

## Goal

收尾一期暂留项：T2 阴影/玻璃拟态抽象、播放页歌词特效跨平台、刮削页共用化。完成后 `core:ui` 安卓库只剩空壳或彻底下线。

## Background（已实测）

- 一期交付：ui-shared 共用模块建成，设置/音源/浏览/曲库四页已共用；桌面复刻页逐个下线。
- T2 暂留三文件：`SaltShadows.kt`（android.graphics.BlurMaskFilter/Paint）、`GlassSurface.kt`（android.os.Build）、`MusesHaze.kt`（Haze 降级策略）。
- 播放页 `PlayerScreen.kt` 约 1242 行，沉浸式歌词（逐词渐变/Blur 距离场/跟手手势/HorizontalPager）是最大硬骨头；一期结论为留待二期。
- 刮削页共约 2700 行（ScrapeScreen 638 + Review 712 + ViewModel 两份约 1000 + 编辑表/队列跟踪约 300），是安卓专属功能，桌面端暂无对应页。
- 约束：全程安卓行为不变；不升级版本线；歌词特效在桌面端允许降级但需用户验收。

## Requirements

- R1：T2 抽象（SaltShadows/GlassSurface/MusesHaze 跨平台化或降级替代）。
- R2：播放页共用化（含歌词特效降级方案，需用户验收 Haze 降级效果）。
- R3：刮削页共用化（或明确桌面端不上刮削，给出书面结论）。

## Acceptance Criteria

- [ ] AC1：T2 文件清零（抽象或下线）。
- [ ] AC2：播放页两端共用或降级方案验收通过。
- [ ] AC3：刮削页有明确结论（共用或不上）。
- [ ] AC4：全量回归通过。

## Out of Scope

- 不改业务逻辑；不升级版本线；不做视觉重设计。

## Open Questions

- Q1：首攻哪个（T2抽象 vs 播放页 vs 刮削页）——见本轮提问。
