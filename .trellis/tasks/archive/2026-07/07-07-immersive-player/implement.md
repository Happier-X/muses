# 沉浸式播放页面实施计划

## Checklist

1. 安装 AMLL 依赖
   - 添加 `@applemusic-like-lyrics/vue`、`@applemusic-like-lyrics/core`、`@applemusic-like-lyrics/lyric`。
   - 确认 Vite/Vue 构建可解析组件和样式。

2. 扩展歌曲元数据模型
   - 更新 `SongItem`、`AudioTags`、`NativeAudioMetadata` 类型，加入 `lyrics`、`lyricsSource`、`coverUri`、`tagsScanned`、`tagsScannedAt`。
   - 更新 `storage.ts` 校验、`upsertSong` 和变更判断，兼容旧数据。
   - 添加或扩展单元测试覆盖旧记录兼容和新字段持久化。

3. 扩展原生标签/封面/歌词读取
   - 引入成熟 Android 音频标签库（优先 `jaudiotagger`）读取标题、歌手、专辑、时长、内嵌歌词和封面。
   - `LocalLibraryPlugin.kt`：基于标签库读取内嵌 LRC、提取封面到私有缓存、查找同目录同名 `.lrc`。
   - `WebDavPlugin.kt`：优先复用 WebDAV 音频缓存文件读取标签；缓存缺失时下载到应用私有缓存后读取标签，查找同目录同名 `.lrc`，提取封面到私有缓存。
   - 确保 WebDAV 密码只在原生边界使用，返回值只包含安全元数据和 `coverUri`。

4. 实现播放时单曲延迟补扫
   - 在播放器 controller 中播放启动后异步补扫当前歌曲。
   - 补扫不阻塞 `AudioPlayerNative.play(...)`，失败只降级状态。
   - 补扫完成后更新 `muses:songs` 与当前播放快照。
   - 增加测试覆盖 WebDAV 密码不进入状态/localStorage、补扫失败不中断播放。

5. 扩展 WebDAV 播放缓存
   - `AudioPlaybackService.kt` 或专用原生 helper 增加 WebDAV 音频缓存目录、缓存索引和最大容量常量（MVP 默认 1GB）。
   - WebDAV 播放时优先命中缓存文件；未命中时继续远程播放并后台下载缓存，下载完成后更新索引。
   - 缓存超过上限时按最近最少使用清理旧文件；不得保存密码、Basic Auth header 或音频内容到 `muses:songs`。
   - WebDAV 标签补扫优先使用缓存文件解析，避免重复下载。

6. 扩展播放器进度与 seek 合约
   - `types.ts` / `native.ts` 增加 `position`、`duration`、`seek`。
   - `AudioPlayerPlugin.kt` 增加 `seek` 方法与 state snapshot 字段。
   - `AudioPlaybackService.kt` 定时广播进度，seek/pause/resume/stop 后立即广播。
   - 通知栏使用播放进度并随状态刷新。

7. 构建沉浸式播放页
   - 新增 `/player` 路由与 `PlayerPage.vue`。
   - 空状态提供返回入口。
   - 信息/控制页展示封面、标题、歌手/专辑、播放/暂停、停止、进度和拖动 seek。
   - 歌词页用 AMLL 展示 LRC，并使用 AMLL 背景的 `meshGradientRenderer`；无歌词时展示空状态。
   - 实现左滑歌词页、右滑信息页。

8. 更新迷你播放器交互
   - 播放条主体点击进入 `/player`。
   - 控制按钮阻止冒泡，避免触发导航。
   - 测试覆盖导航与按钮行为。

9. 验证与修复
   - 运行 `npm run test:unit -- --run`。
   - 运行 `npm run lint`。
   - 运行 `npm run build`。
   - 运行 `cd android && ./gradlew :app:compileDebugKotlin`。
   - 在模拟器/真机手工验证播放页、滑动、seek、通知进度、延迟补扫和敏感信息不泄露。

## Risky Files

- `android/app/src/main/java/ionic/muses/AudioPlaybackService.kt`：前台服务、通知、进度定时器和 WebDAV 缓存下载容易引入 ANR、资源泄漏或重复下载。
- `android/app/src/main/java/ionic/muses/WebDavPlugin.kt`：必须避免认证信息泄露，并控制远程读取范围和缓存文件生命周期。
- `src/features/library/storage.ts`：本地库兼容旧数据，不能误删旧歌曲。
- `src/features/player/controller.ts`：播放启动与异步补扫需要避免竞态，不能让旧歌曲补扫覆盖新歌曲状态。
- AMLL 组件集成：若包导出或样式与预期不同，需要回滚或重新评估 AMLL 集成方式。

## Validation Commands

```bash
npm run test:unit -- --run
npm run lint
npm run build
cd android && ./gradlew :app:compileDebugKotlin
```

## Review Gate

实现前需要确认：

- PRD、设计和实施计划已覆盖真实封面、LRC、延迟补扫、进度/seek、通知进度。
- `implement.jsonl` 和 `check.jsonl` 已包含真实上下文条目。
- 用户批准从 planning 进入 implementation。
