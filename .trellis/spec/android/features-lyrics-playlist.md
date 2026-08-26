# 特征·歌词 / 播放列表 / 响度均衡 — 开发规范（M2）

> 适用于 `native/` 工程的 AMLL 歌词渲染、播放列表管理、响度均衡相关改动。Web 层对应契约见 `spec/frontend/features-player.md`，本文是其原生等价实现 + M2 新增事实。

---

## 范围 / 触发条件

- 改动 `feature/player/lyric/*`（AmllWebView、LyricsParser、AmllMapper）、`frontend/amll-web/`（AMLL 前端页面）
- 改动 `feature/playlist/*`、播放列表 Room 表
- 改动响度均衡链路（LoudnessCalculator/LoudnessController/replayGainTrackDb 列）

## 1. AMLL 渲染 = WebView 内嵌官方 core（定案，勿换）

- **方案**：Vite 打包 `@applemusic-like-lyrics/core` 进 APK assets（`native/frontend/amll-web/`，产物 → `feature:player/src/main/androidAssets/amll/`）→ Compose `AndroidView` 包 WebView → `WebViewAssetLoader` 以 `https://appassets.androidplatform.net/assets/amll/index.html` 加载 → `evaluateJavascript` 注入。
- **禁止**用销毁/重建 WebView 控制暂停；**禁止**引入第二套歌词渲染栈（accompanist lyrics-ui 仅作 fallback 方案保留在调研记录）。
- 一个 WebView 页面同时承担**歌词 + 流体背景**双职责（BackgroundRender 由 PIXI 内部 ticker 自驱动）。

### 踩坑记录（92bf1a2，P4.3 歌词面板修复）

1. **androidAssets 目录必须显式注册为 assets 源**：`feature/player/build.gradle.kts` 加 `assets.srcDir("src/main/androidAssets")`——M1 起该目录从未注册，WebViewAssetLoader 找不到 index.html → ERR_INVALID_RESPONSE，且报错被 WebView 白屏吞掉难定位
2. **WebView 尺寸自适应用 ResizeObserver 不用 window.resize**：Android WebView 初始布局高度为 0，后续 AndroidView 获得真实尺寸不再派发 resize → 背景 canvas 高度 0 永不可见；amll-web 侧 `new ResizeObserver(resize).observe(document.body)` 兜底
3. **AndroidView 嵌入面板区域时 offset 位移要加在 AndroidView 自身而非父容器**（父容器位移会连带裁剪/命中区域错位）；背景歌词解耦 = 背景层与歌词层各自独立 AmllWebView 实例

### 桥接口签名（window 级，前端 `amll-web/src/main.ts` ↔ Kotlin `AmllWebView.kt`）

| JS 接口 | 入参 | 调用时机 |
|---|---|---|
| `updateLyrics(payload: string)` | JSON 字符串 `{lines, coverUrl, songId}` | 页面 ready 后首次 + 每次切歌 |
| `updatePosition(positionMs: number)` | ms 数值 | VM 侧 ~100ms 轮询节流，仅 isPlaying 时发射 |
| `pauseRender()` / `resumeRender()` | 无 | Lifecycle ON_STOP / ON_START |

- **payload 注入必须经 `AmllMapper.quote()` 包成 JS 字符串字面量**——前端内部做 `JSON.parse`，直接内插对象会被 ToString 成 `[object Object]`。
- `songId` token：前端校验过期注入丢弃。

## 2. 背景生命周期治理（继承 Web 层 spec，原生等价）

- 背景不得因「无歌词」卸载：无词时 payload.lines 为空数组照发（不是 null），前端显示空态占位、背景照常渲染。
- ON_STOP → `pauseRender()`；ON_START → `resumeRender()`。observer 在 `DisposableEffect` 注册/移除成对。
- 粘性封面三段语义（PlayerViewModel.stickyCover）：新曲有封面即更新；无封面**沿用旧值**；仅无当前曲才清空。
- `coverUrl=null` 表示粘性沿用，前端不清旧封面。

## 3. 封面加载：混合内容规避

- 页面源是 https，`file://` 封面会被混合内容策略拦截。
- 统一经 `coverUriToAppAssetsUrl(uri, cacheDirPath)`：cacheDir 下文件映射为 `https://appassets.androidplatform.net/cache/...`（自定义 PathHandler 服务，含 canonicalPath 目录穿越防护）；data:/http(s) 透传；无法映射返回 null 走粘性。

## 4. 歌词解析（lyrics-core 0.4.7 API 事实）

```kotlin
AutoParser()                    // 无 Builder；可传 PhoneticProvider
parse(raw): SyncedLyrics?       // 自动识别 TTML/LRC/YRC/KRC/LSY
```

- **0.4.7 对不可识别文本不抛异常而是返回空行集** → `LyricsParser.parse` 已归一化：失败或空行集一律返回 null。
- 0.4.7 **无 Android target**（JVM/iOS/JS/wasm），以 JVM 变体参与构建；其 TTML 解析为自实现（无 javax.xml 依赖），Android 可用。
- 行模型两态：`KaraokeLine`（syllables 逐词 + translation + phonetic；`KaraokeLine.AccompanimentKaraokeLine` 为背景行）与 `SyncedLine(content, translation, start, end)`（LRC 整行，**EnhancedLrcParser 已自动做同时间戳双语配对**）。
- 升级版本时重点核对：AutoParser 构造方式、SyncedLyrics.lines 元素类型、KaraokeLine.getTranslation/getPhonetic。

### AmllMapper 输出契约

- 目标结构字段名必须与 AMLL core 0.5.2 一致：`words[{startTime,endTime,word}] / startTime / endTime / translatedLyric / romanLyric / isBG / isDuet`（ms 单位）。
- 手写 JSON 序列化（不引 kotlinx-serialization）；转义用 `quote()`。
- 播完钳制在 Kotlin 侧：发送 `min(positionMs, lastLine.endTime)`，规避「播完全行失活模糊」。
- 解析失败降级：空行数组 payload，**不是不发**。

## 5. 播放列表（Room v2+）

```
playlists(id PK, name, createdAt, updatedAt)
playlist_songs(playlistId FK→playlists CASCADE, songId FK→songs CASCADE, position)
               PK(playlistId, position) + INDEX(songId)
```

- 排序 = position 连续 0..n-1；删除歌曲后 `removeSongAndCompact` 紧凑重排；reorder 用**两阶段平移**（+100_000 再写回）避复合 PK 冲突，单事务。
- 整体入队：`PlaylistRepository.getSongs(id)` → `PlayerConnection.play(first.id, songs)`；空列表早退。
- 迁移只向前追加，不改既有表；新增列用 `ALTER TABLE ... ADD COLUMN ... DEFAULT NULL`。

## 6. 响度均衡（服务侧应用）

- **音量必须设在服务侧 ExoPlayer 上**（`PlaybackService` 的 player）；MediaController 无 volume 能力。`PlaybackService` 已 `@AndroidEntryPoint`，onCreate 组装 `LoudnessController(player, settingsRepository, songDao, serviceScope)`，onDestroy **先 stop controller/serviceScope 再 release player**。
- 计算语义（照搬 Web 层 loudness.ts）：`volume = clamp(10^((db+6)/20), 0.1, 1.0)`；关闭或 gain=null → 1.0；超 ±30dB 先走 Q7.8 ÷256 兜底换算，仍越界才丢弃。
- 切歌必重算（onMediaItemTransition），禁止串曲增益；开关变化即时对当前曲重设。写入 volume 必须**单飞**（取消上一个在途 applyJob 再启动，防快速切歌乱序覆盖）。
- 默认关（DataStore `loudness_enabled`）；设置页 UI 入口留 M3。
- RG 数据链路：TagReader 别名扫描/TXXX → normalize（÷256 + 校验）→ SongEntity.replayGainTrackDb → LoudnessController 按 mediaId 反查。

## 测试要点

- LyricsParserTest：TTML 样本解析、双语 LRC 配对、非法输入（含乱文本返回 null 不抛）
- AmllMapperTest：逐词映射窗口一致性、translation 直挂、JSON 字段名与转义、coverUrl=null 字面量
- PlaylistDaoTest / PlaylistRepositoryTest：CRUD、去重追加、紧凑重排、双 CASCADE
- LoudnessCalculatorTest：Q7.8 换算、边界兜底、clamp 双端、开关/无标签恒 1.0

## 错误行为矩阵

| 场景 | 正确行为 |
|---|---|
| 歌词解析失败/空 | null → 空 payload，背景照常渲染 |
| payload JSON 含引号/换行 | quote() 转义后嵌入 JS 字符串字面量 |
| 封面为 file:// 非 cacheDir | 映射返回 null → 前端粘性沿用 |
| RG 标签非法（换算后仍超 ±30） | 丢弃不入库，播放按无标签处理 |
| 快速连点切歌 | 单飞 applyJob 取消旧查询，最终一致为新曲增益 |
