# 沉浸式播放页面技术设计

## Scope

本任务在既有点击播放能力上扩展沉浸式播放体验：迷你播放条导航、播放页双面板、AMLL 歌词/背景、真实封面、播放进度/seek、Android 通知进度、WebDAV 播放缓存，以及播放时单曲延迟补扫标签。

## Architecture

### 前端边界

- 保持 `src/features/player/controller.ts` 的 feature-local `reactive<PlayerState>`，不引入 Pinia/Vuex。
- 新增沉浸式播放页视图，例如 `src/views/PlayerPage.vue`，通过路由 `/player` 打开。
- `MiniPlayer.vue` 播放条主体负责导航到 `/player`，控制按钮使用事件阻止冒泡避免误触导航。
- 播放页使用 Ionic/Vue 手势或滑动组件实现两个横向页面：信息/控制页与歌词页。
- AMLL 依赖使用官方包：`@applemusic-like-lyrics/vue`、`@applemusic-like-lyrics/core`、`@applemusic-like-lyrics/lyric`，MVP 解析 LRC 并交给 AMLL 渲染。
- 沉浸式播放页背景使用 AMLL 背景能力，渲染器明确使用 `meshGradientRenderer`；没有歌词时展示静态背景。

### 播放状态扩展

`PlayerState` 增加：

- `position`: 当前播放秒数。
- `duration`: 总时长秒数。
- `lyrics`: 当前歌曲 LRC 文本或解析结果状态。
- `coverUri`: 原生缓存封面引用。
- `metadataStatus`: `idle | scanning | ready | failed`，用于延迟补扫展示。

`AudioPlayerNativeState` 增加：

- `position`。
- `duration`。
- `bufferedPosition` 可选。

`AudioPlayerNative` 增加：

- `seek({ position: number }): Promise<void>`。
- 原生 `stateChange` 广播携带进度字段。

### 歌曲元数据扩展

`SongItem` 增加可选字段：

- `lyrics?: string`：标签内置 LRC 文本或同目录 `.lrc` 内容。
- `lyricsSource?: 'embedded' | 'sidecar'`。
- `coverUri?: string`：应用私有缓存文件 URI。
- `tagsScanned?: boolean`：是否已完整扫描标签/歌词/封面。
- `tagsScannedAt?: string`。

`muses:songs` 只保存元数据与 `coverUri` 引用，不保存封面 base64、WebDAV 密码、Basic Auth header 或 SecureStorage 值。

### 延迟补扫

播放启动后异步执行单曲补扫：

1. `playSong(song)` 立即调用原生播放，不等待标签补扫。
2. 如果歌曲 `tagsScanned !== true` 或缺少歌词/封面，则启动后台补扫。
3. 本地歌曲通过 `LocalLibrary.readMetadata` 扩展读取 LRC 与内嵌封面缓存。
4. WebDAV 歌曲通过 SecureStorage 取密码，仅传入 `WebDav.readMetadata` 或播放缓存下载的原生边界；原生层读取标签、LRC、封面缓存，返回安全元数据。
5. 查找同目录同名 `.lrc`：本地通过原生 SAF 兄弟文件读取；WebDAV 通过同目录 URL 拼接和 OkHttp GET 读取。
6. 使用 `upsertSong` 或专用更新方法保存补扫结果，并同步当前 `PlayerState.currentSong` 展示字段。
7. 补扫失败只设置 `metadataStatus='failed'` 或降级状态，不中断播放。

歌词优先级：

1. 标签内置 LRC。
2. 同目录同名 `.lrc`。
3. 无歌词状态。

### 封面与音频缓存

- 原生层从音频标签中提取封面图片，写入应用私有缓存目录，例如 `cacheDir/covers/<songId>.jpg`。
- WebDAV 播放时将远程音频缓存到应用私有缓存目录，例如 `cacheDir/webdav-audio/<hash>.<ext>`；缓存命中时播放使用本地文件 URI。
- WebDAV 音频缓存需要维护索引（URL/hash、文件路径、大小、最后使用时间），并设置最大缓存容量；超过容量时按最近最少使用清理旧文件。
- 默认最大缓存容量由原生常量控制，MVP 可先设为 1GB；后续可暴露设置项。
- 前端仅接收并保存 `coverUri` 等安全元数据，不保存音频缓存路径到 `muses:songs`；播放缓存由原生层内部管理。
- 如果缓存文件缺失，播放/补扫流程允许重新下载或重新提取。
- WebDAV 封面和音频缓存读取在原生边界完成，认证信息不返回前端、不写日志。

### 进度与通知

- `AudioPlaybackService` 使用 ExoPlayer 当前 `currentPosition` 与 `duration` 生成状态快照。
- 播放中以固定频率广播进度，建议 500ms–1000ms；暂停/seek/停止时立即广播。
- `seek(position)` 将秒数转换为毫秒并调用 ExoPlayer `seekTo`。
- 通知使用 `NotificationCompat.Builder.setProgress(duration, position, false)`，播放/暂停/seek 时更新通知。
- 对未知 duration 或直播式 duration，通知和 UI 显示非确定/禁用进度拖动。

## Data Flow

1. 用户点击 `SongsPage.vue` 中歌曲。
2. `playSong(song)` 设置当前歌曲快照并调用 `AudioPlayerNative.play(...)`。
3. WebDAV 播放请求进入原生服务后先检查音频缓存；命中则播放缓存文件，未命中则继续远程播放并后台写入缓存。
4. 原生 `AudioPlaybackService` 启动播放，广播状态与进度。
4. 前端 controller 接收 `stateChange`，更新播放状态、进度和时长。
5. controller 异步触发单曲补扫，补扫完成后更新 `muses:songs` 和当前歌曲展示字段。
6. WebDAV 补扫可复用已缓存音频文件进行标签解析，避免重复下载。
7. `MiniPlayer.vue` 常驻显示；点击主体导航 `/player`。
8. `PlayerPage.vue` 消费 `playerState` 展示封面、控制、进度和 AMLL 歌词，并用 AMLL 背景的 `meshGradientRenderer` 渲染沉浸背景。

## Compatibility

- 旧 `muses:songs` 记录没有新字段时仍视为合法歌曲；新增字段全部可选。
- 旧播放器状态没有进度字段时前端降级为 `0 / duration unknown`。
- 没有可解析歌词时显示无歌词状态。
- 没有封面时显示占位封面。

## Security

- WebDAV 密码只从 SecureStorage 读取并传入原生方法。
- 不把密码、Basic Auth header、SecureStorage 值放入 `PlayerState`、路由参数、`muses:songs`、日志或 UI。
- 不把封面 base64 或 WebDAV 音频缓存内容写入 `muses:songs`。
- 原生错误需要映射为安全中文错误，不向前端透传认证 header 或密码。
- WebDAV 音频缓存文件仅位于应用私有缓存目录；缓存索引不得保存密码或 Basic Auth header。

## Trade-offs

- LRC MVP 先提供行级歌词同步；AMLL 支持更多格式，但 `.ttml`/逐词歌词后续再扩展。
- 延迟补扫保证播放先开始，但 UI 可能先显示旧元数据，随后刷新。
- WebDAV 首次播放仍可能依赖网络；缓存写入完成后的后续播放可快速启动，但会占用应用缓存空间，需要 LRU 上限。
- 通知进度增加原生定时更新复杂度，但满足后台播放体验需求。

## Rollback

- 如果 AMLL 集成或 `meshGradientRenderer` 出现兼容问题，可回滚本任务中的 AMLL 相关提交，保留已有迷你播放器和基础播放能力。
- 如果延迟补扫失败率高，可先禁用补扫触发，保留已有扫描入库结果。
- 如果通知进度导致系统兼容问题，可保留通知控制按钮并关闭 `setProgress` 更新。
