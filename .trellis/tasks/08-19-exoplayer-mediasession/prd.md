# 用 ExoPlayer MediaSession 替换 capgo media-session

## Goal

移除 `@capgo/capacitor-media-session` 依赖，使用 ExoPlayer Media3 内置的 MediaSession API 实现通知栏播控、锁屏控制和媒体按钮事件。

## Background

### 当前架构
- 播放引擎：ExoPlayer Media3（已迁移完成）
- 通知栏：`@capgo/capacitor-media-session` 插件
- JS 层：`mediaSession.ts` 调用 capgo 插件同步状态

### 问题
1. **依赖冗余**：ExoPlayer 本身已内置 MediaSession 支持，不需要额外插件
2. **状态同步复杂**：JS 层需要手动同步播放状态到 capgo 插件
3. **通知栏不稳定**：spec 记录了 capgo 插件在某些场景下通知不稳定

### 参考
- ExoPlayer Media3 `MediaSession` API
- Android `MediaSessionService` + `DefaultMediaNotificationProvider`

## Scope

### In Scope
- Android 原生层：在 ExoPlayer 中创建 MediaSession
- Android 原生层：使用 Media3 的 DefaultMediaNotificationProvider 实现通知栏
- Android 原生层：处理媒体按钮事件（耳机按键）
- JS 层：移除 `mediaSession.ts` 中的 capgo 调用
- JS 层：简化播放状态同步逻辑
- 移除 `@capgo/capacitor-media-session` 依赖

### Out of Scope
- 自定义通知栏样式（使用 Media3 默认样式）
- iOS 端适配
- 桌面歌词

## Acceptance Criteria

1. **通知栏**：显示歌曲信息、播放/暂停/上下首按钮
2. **锁屏控制**：锁屏界面显示媒体控制
3. **媒体按钮**：耳机按键可控制播放/暂停/上下首
4. **状态同步**：播放状态正确同步到系统 MediaSession
5. **后台播放**：应用进入后台后通知栏正常工作
6. **无 capgo 依赖**：完全移除 `@capgo/capacitor-media-session`

## Key Decisions

1. 使用 ExoPlayer Media3 的 `MediaSession` API
2. 使用 `MediaSessionService` 作为前台服务
3. 使用 `DefaultMediaNotificationProvider` 自动管理通知栏
4. JS 层不再需要手动同步 MediaSession 状态
5. Service 持有 ExoPlayer 实例，Plugin 通过 MediaController 控制

6. JS 层完全移除 MediaSession 相关代码，封面转换和状态同步全部移到原生层
7. Plugin 通过 MediaController 绑定 PlaybackService，只做播放控制桥接

## Open Questions

无
