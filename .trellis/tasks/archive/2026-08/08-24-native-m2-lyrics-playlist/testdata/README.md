# M2 歌词测试样本

供阶段 1 单测（LyricsParser / AmllMapper）使用，实现时复制到对应模块 `src/test/resources/lyrics/`。

| 文件 | 用途 |
|---|---|
| `sample1.ttml` | TTML 逐词（含 ttm:agent 对唱标记 v1/v2） |
| `sample2.ttml` | TTML 逐词（另一首，验证多样本解析） |
| `bilingual.lrc` | 双语 LRC：同时间戳原文+译文成对（同时间戳相邻配对路径） |
| （无歌词歌曲） | 不需要文件：`Song.lyrics = null` 即空态用例 |

来源：sample1/sample2 取自 amll-dev/amll-ttml-db `raw-lyrics/`（2026-08-24）。

## 阶段 0 完成记录

- [x] M1 进度确认：native/ 脚手架已在 main；minSdk=26（非 PRD 所写 24）
- [x] SongEntity 字段核对：**缺** lyrics / lyricsFormat / replayGainTrackDb / coverUri → 阶段 1 首个任务
- [x] amll-web Vite 工程搭建并构建通过：
  - `@applemusic-like-lyrics/core` 0.5.2
  - 四个桥接口：updateLyrics / updatePosition / pauseRender / resumeRender
  - typecheck + vite build 通过，产物在 `native/feature/player/src/main/androidAssets/amll/`（index.html + assets，相对路径引用）
  - API 事实修正：BackgroundRender 无公开 update()（Pixi ticker 自驱动）；构造用 `new BackgroundRender(new PixiRenderer(canvas), canvas)`；样式导入路径为 `@applemusic-like-lyrics/core/style.css`
- [x] 测试样本准备（本目录）
- [ ] lyrics-core 登记到 gradle/libs.versions.toml + 编译验证（待做，可与阶段 1 Song 字段补充一起）
- [ ] minSdk 决策点：26 vs DroidMate 29；WASM 兼容需真机验证（阶段 1 真机回归时确认）

## Gradle exec 任务串联（待办备忘）

amll-web 的 npm build 目前手动执行；后续需要在 feature:player/build.gradle.kts 加 exec 任务：
`npm install --prefix ../frontend/amll-web && npm run build --prefix ../frontend/amll-web`
挂到 preBuild 或 mergeAssets 之前，CI 缓存 node_modules。
