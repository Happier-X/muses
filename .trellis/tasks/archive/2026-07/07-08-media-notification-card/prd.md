# 媒体播放通知栏卡片

## Goal

媒体播放时，系统通知栏显示标准的媒体通知卡片（封面 + 标题 + 艺术家 + 进度条 + 播放/暂停按钮），而不是当前那个最小化的「正在准备播放器」服务通知。

## Background

现状：`AudioPlaybackService` 继承 media3 的 `MediaSessionService`，并用 `DefaultMediaNotificationProvider` 注册了媒体通知 provider。理论上 media3 会自动渲染带封面的媒体卡片，但实际没有出现完整卡片，原因：

1. **封面（artwork）未设置**：`playFromIntent` 构建 `MediaItem` 时，`MediaMetadata.Builder()` 只 set 了 title/artist/album，没有 `setArtworkUri`。media3 默认通知在缺少 artwork 时不会渲染大封面媒体卡片。
2. **封面数据已存在但未透传**：`AudioMetadataReader` 已经把嵌入式封面写成 `file:///data/.../cache/covers/<hash>.jpg` 并通过 `coverUri` 返回前端；但 `buildPlayOptions` / `AudioPlayerPlugin.play` / `AudioPlaybackService.EXTRA_*` 这条链路完全没有传递 coverUri。
3. **通知 ID 冲突**：`MEDIA_NOTIFICATION_ID = BOOTSTRAP_NOTIFICATION_ID = 1001`，bootstrap 通知与 media 通知共用一个 ID，部分设备上会出现覆盖/不刷新问题。
4. **POST_NOTIFICATIONS 未运行时授权**：Android 13+（本项目 `targetSdk=36`）需要运行时请求 `POST_NOTIFICATIONS` 权限，否则通知栏不显示媒体卡片（只剩最小化服务通知）。当前代码无任何运行时权限请求逻辑。

## Requirements

### 功能需求

- R1：媒体播放时，通知栏显示标准媒体卡片，包含：封面图、标题、艺术家（若有）、进度条、播放/暂停按钮（及上一首/下一首如队列支持）。
- R2：暂停/继续、停止时，通知卡片状态与播放状态同步。
- R3：切歌时，通知卡片的封面/标题/艺术家随之更新。
- R4：封面对应的 `file://` URI 能被 media3 读取并显示（需处理 FileProvider / content URI 或 file 权限）。
- R5：Android 13+ 在首次需要播放时，引导用户授予 `POST_NOTIFICATIONS` 权限；已授权则直接显示卡片。

### 技术约束

- C1：不破坏现有本地/WebDAV 两种音源的播放流程。
- C2：保持 media3 `MediaSessionService` + `DefaultMediaNotificationProvider` 架构，不引入自定义 RemoteViews 通知（用系统标准媒体卡片即可）。
- C3：封面 URI 链路：前端 `song.coverUri`（file:// 本地缓存）→ Capacitor `play({ coverUri })` → `AudioPlayerPlugin` → `AudioPlaybackService` Intent extra → `MediaMetadata.setArtworkUri`。
- C4：`data:` base64 封面不透传（与现有 `toSafeCoverUri` / storage 过滤一致）；只有 `file://` / `content://` 透传。
- C5：保持 bootstrap 通知 ID 与 media 通知 ID 相同（由 media3 覆盖 bootstrap，避免残留）。

### 非目标（Out of Scope）

- 不做自定义通知布局（RemoteViews）。
- 不做锁屏控件 Widget。
- 不处理通知点击跳转到 app 的深链（media3 默认行为已够用）。
- 不改 WebDAV 封面在线获取（封面已由 metadata 扫描阶段缓存为本地文件）。

## Acceptance Criteria

- [ ] AC1：本地音源播放时，通知栏出现媒体卡片，显示该歌曲封面、标题、艺术家、进度条。
- [ ] AC2：WebDAV 音源播放时（封面已扫描缓存），通知栏媒体卡片同样显示封面/标题/艺术家/进度条。
- [ ] AC3：点通知卡片播放/暂停按钮，播放状态正确切换且卡片按钮图标同步。
- [ ] AC4：切歌时，通知卡片封面与标题更新为新歌曲。
- [ ] AC5：Android 13+ 首次播放时弹出 POST_NOTIFICATIONS 授权；授权后显示媒体卡片，拒绝后仍能播放但不显示媒体卡片（只有最小服务通知）。
- [ ] AC6：无封面的歌曲（metadata 无嵌入图），通知卡片仍显示标题/艺术家/进度条，封面位为默认占位或留空，不崩溃。
- [ ] AC7：播放→停止→再播放，通知卡片正常刷新无残留。
- [ ] AC8：media3 的 MediaSession 正常工作，蓝牙/有线耳机按键、车机等外部控制器仍可控制播放（回归不破坏）。

## Notes

- 封面文件已由 `AudioMetadataReader.writeCover` 写到 `cacheDir/covers/<sha256>.jpg`，返回 `Uri.fromFile(file).toString()`，即 `file://` URI。media3 `setArtworkUri` 接受 `file://` URI，应可直接用，无需 FileProvider 转换（同 app 进程内 file 可读）。
- `DefaultMediaNotificationProvider` 在 1.7.1 版本下，只要 `MediaItem.mediaMetadata.artworkUri` 存在且可加载，会自动渲染大封面媒体卡片。
- 运行时权限：本项目前端无 @capacitor/local-notifications 等权限桥，需用 `@capacitor/android` 的 Permissions 或直接在 MainActivity 用 `ActivityResultContracts.RequestPermission` 请求。需在 design 阶段确定方案。
