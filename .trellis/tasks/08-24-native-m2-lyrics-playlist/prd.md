# M2：歌词与播放列表（父任务 08-24-native-compose-rewrite 的子任务）

> 需求全集、架构决策（D1-D4）与全局设计见父任务 `../08-24-native-compose-rewrite/`（prd.md / design.md）。本文件只记录 M2 特有的范围与验收。

## Goal

在 M1 原生基座上交付：AMLL 风格歌词展示（方案落地）、播放列表管理、响度均衡（ReplayGain）。UI 延续 Salt Player 风格。

## 已确认事实（代码库证据）

### 现有 Web 层歌词实现（src/features/lyrics/，约 1800 行 TS）

- 多源 provider：kg / kw / lrclib / mg / qrc / tx / wy（含 wyCrypto 网易加密握手），统一 `providers/types.ts` 接口
- AMLL TTML DB 直查（amllTtmlDb.ts，374 行）
- 匹配链：match.ts（102 行）→ score.ts 打分（192 行）→ normalize.ts 归一化 → mergeTranslation.ts 翻译/音译合并（442 行）
- 渲染：PlayerPage.vue 用 `@applemusic-like-lyrics/vue` 的 LyricPlayer 组件

### 现有 Web 层响度均衡（src/features/player/loudness.ts）

- ReplayGain 标签解析：支持 `-6.54 dB` 格式；Opus R128_TRACK_GAIN 的 Q7.8 整数按 ÷256 换算
- 合理 track gain 范围 ±30 dB，非法值返回 null 禁止写假增益
- 开启时叠加 +6 dB preamp（#51），最终 clamp 到音量 [0.1, 1.0]

### 播放列表（src/features/playlist/）

- 数据模型极简：`{ id, name, songIds[], createdAt, updatedAt }`，localStorage 存储

### AMLL 背景生命周期契约（spec/frontend/features-player.md，原生重写必须继承）

- BackgroundRender 不得因「无歌词」卸载；有当前曲且有封面即渲染
- App 切后台/熄屏必须 pause 渲染循环（Web 层实测后台 CPU 40-71%），恢复可见且播放页开着才 resume
- 切歌新曲无封面时短时粘性使用上一首封面，避免闪回 fallback 背景
- 禁止用 amll-vue 的 `playing` prop 控制暂停（上游逻辑写反）
- 运行时缓存有界（LRU ≤256 条），禁止无限增长 Map
- 歌词播完失活问题：renderTime 超过末句 endTime 导致全部行模糊，需钳制

### AMLL 安卓落地参考（2026-08-24 调研 Zeehan2005/AMLL-DroidMate）

- AMLL 官方仅有 JS 实现（core/lyric 包）；DroidMate 方案 = Vite 打包 AMLL core 进 APK assets → Compose 包装 WebView → `WebViewAssetLoader` 加载 → `evaluateJavascript` 注入歌词 JSON/进度（100ms 节流）/封面
- dokar3/amlv 为第三方 Compose 原型（父任务已记录）
- 父任务技术调研项倾向：自研逐行歌词渲染组件（Compose 动画能力足够），design 阶段定案

## Requirements

### R1 歌词展示（2026-08-24 定案：内嵌官方 AMLL，WebView 桥接）

> 开发者最终决策：采用「Vite 打包 @applemusic-like-lyrics/core 进 APK assets → Compose 包装 WebView → evaluateJavascript 注入」的嵌入式方案（DroidMate 同款路线）。理由：渲染观感与官方 AMLL 完全一致，且流体背景直接用 AMLL BackgroundRender，无需自研/移植。

- 歌词 + 流体背景均在同一 WebView 页面内由 AMLL core 渲染；Compose 侧只负责数据供给与生命周期治理
- 解析器：仍用 `com.mocharealm.accompanist:lyrics-core`（仅解析不渲染；spike 已验证 API），输出映射为 AMLL LyricLine[] JSON 后注入 WebView；或改用 AMLL 官方 `@applemusic-like-lyrics/lyric` 在前端解析（design 阶段二选一，倾向前者——Kotlin 侧单测可控）
- accompanist lyrics-ui spike 已验证效果达标但被弃用，保留作为纯原生 fallback 方案（spike 代码在 `C:\code\amll-lyrics-ui-spike`）

### R2 歌词数据消费（2026-08-24 已确认：不做在线匹配）

- 决策：多源在线匹配已归入刮削流程（M3）；**播放时不做任何在线请求**
- M2 仅消费歌曲记录中已有的歌词字段：`lyrics` / `lyricsFormat` / `lyricsSource`
- 数据来源两条（均已存在于现有架构）：① 扫描时内嵌标签（`AudioMetadataReader.firstLyricsValue`，source=embedded）；② 刮削/云元数据写回
- M2 职责 = 解析（TTML 逐词 / LRC 逐行）+ 归一化为渲染组件输入结构；翻译合并逻辑按现有 mergeTranslation 语义简化移植

### R3 播放列表管理

- 播放列表 CRUD、详情页、歌曲增删排序
- 从歌曲/专辑列表加入播放列表；播放列表整体入队播放

### R4 响度均衡

- ReplayGain/R128 标签解析（Q7.8 ÷256 规则照搬）；注意旧原生层 `AudioMetadataReader.kt` 已实现 `parseReplayGainTrackDb`，M1 扫描入库时应已携带 `replayGainTrackDb` 字段，M2 重点是播放内核侧的应用而非解析
- 播放内核按 track gain 应用增益，+6 dB preamp 语义保持，clamp 行为保持
- 设置开关持久化

## Acceptance Criteria

- [ ] 播放页歌词随进度滚动，TTML 逐词有卡拉OK染色；翻译/音译可开关；无歌词歌曲不崩且背景正常渲染
- [ ] AMLL 背景 App 切后台时渲染循环暂停（CPU 回落），恢复后正常；切歌无封面时粘性使用上一首封面
- [ ] LRC 歌词逐行高亮滚动正常；renderTime 超过末句 endTime 时表现正常（钳制）
- [ ] 播放列表可创建/重命名/删除，可添加/移除/拖动排序歌曲；从歌曲/专辑列表可加入播放列表；播放列表整体入队
- [ ] 响度均衡开启后有可感知的响度对齐；非法 gain 不炸音（clamp 行为保持）；设置开关重启后保留
- [ ] `cd native && ./gradlew lint testDebugUnitTest :app:assembleDebug` 全绿

## 关键决策

- **M2-R1 不做在线匹配**（2026-08-24）：在线匹配归刮削；播放时零网络依赖，仅消费歌曲记录歌词字段。
- **M2-R2 AMLL 方案 = 内嵌官方 AMLL（WebView 桥接，DroidMate 同款）**（2026-08-24）：开发者决策；歌词与背景均由 WebView 内 AMLL core 渲染；accompanist spike 达标但弃用，降级为 fallback。

