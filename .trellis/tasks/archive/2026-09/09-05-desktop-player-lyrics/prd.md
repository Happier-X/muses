# 桌面播放页升级-歌词展示

## Goal

桌面播放页接歌词展示：复用安卓 SimpleLyricsPanel（上收共用）+ 曲库歌词数据链（song.lyrics 字段 → 解析 → 渲染），桌面在线歌词可用。

## Background（已实测）

- 桌面播放页现状 176 行简版（封面占位+标题+进度+控制），无歌词。
- 安卓播放页歌词链路：`song.lyrics`（刮削写回落库，Room 实体已有 lyrics/lyricsFormat/lyricsSource 三字段）→ `LyricsParser.parseDocument` → 歌词面板渲染。
- `SimpleLyricsPanel.kt` 236 行，依赖仅 Compose + coroutines + 数学库，零平台 API，可直接上收共用。
- 歌词引擎（LyricsMatcher/AMLL/五源/LRCLIB/解析器）已全量在 core:common jvmShared（lyrics-kmp 任务交付），桌面可直接调用；在线匹配作为无库歌词时的补充链路。
- 桌面 DesktopScrapeGraph 已有完整歌词网络链装配（可复用其单例）。
- 约束：安卓行为不变；不升级版本线；桌面歌词首版不做逐词卡拉OK特效（NativeLyricsPanel 为安卓专属，特效属后续）。

## Requirements

- R1：SimpleLyricsPanel 上收 core:common jvmShared（同包名），安卓 feature:player 改透传消费，行为零变化。
- R2：桌面播放侧歌词数据链——DesktopPlayerHook 读 song.lyrics/lyricsFormat → LyricsParser.parseDocument → 面板渲染；当前播放曲联动。
- R3：无库歌词时桌面"在线搜索"按钮——调 LyricsMatcher（AMLL+五源+LRCLIB），命中后展示（可选落库，对齐安卓"歌词持久化"语义，视改动面）。
- R4：桌面播放页布局升级——封面/歌词双面板（对齐安卓沉浸页双面板语义的桌面简化版），歌词区滚动+当前行定位。

## Acceptance Criteria

- [x] AC1：桌面播放页展示当前曲歌词，随播放进度滚动定位（LRC/逐行时间轴）。
- [x] AC2：无库歌词时在线搜索可用并展示结果。
- [x] AC3：安卓播放页行为零变化（SimpleLyricsPanel 上收后截图/回归对照）。
- [x] AC4：全量回归通过（三端编译 + 单测）。

## Out of Scope

- 逐词卡拉OK特效/Blur 距离场（NativeLyricsPanel 桌面化，后续任务）。
- 歌词翻译行（安卓有翻译 FAB，桌面首版不做）。

## Key Decisions

- D1：SimpleLyricsPanel 上收共用而非桌面重写（236 行纯 Compose，复用成本低，两端一致性强）。
- D2：歌词数据链走库字段优先（对齐安卓），在线匹配仅作补充按钮（不自动联网）。
