# 执行计划：用 ExoPlayer MediaSession 替换 capgo media-session

## 前置条件

- [ ] 确认 Android SDK 版本 ≥ 29 (Android 10)
- [ ] 确认 Media3 Session 依赖已添加
- [ ] 备份当前 `AudioPlayerPlugin.kt` 和 `mediaSession.ts`

## 阶段 1：依赖配置

### 1.1 更新 build.gradle
```bash
# android/app/build.gradle 添加依赖
implementation "androidx.media3:media3-session:1.5.0"
```

### 1.2 更新 AndroidManifest.xml
```xml
<!-- 添加 PlaybackService -->
<service
    android:name=".PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

### 1.3 移除 capgo media-session 依赖
```bash
pnpm remove @capgo/capacitor-media-session
```

**验证**：`cd android && ./gradlew assembleDebug` 编译通过

---

## 阶段 2：创建 PlaybackService

### 2.1 创建 PlaybackService.kt
```kotlin
// android/app/src/main/java/com/muses/player/PlaybackService.kt
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
```

**验证**：Service 可启动，无崩溃

### 2.2 移动 AudioDeviceCallback 到 PlaybackService
将 `AudioPlayerPlugin` 中的设备移除暂停逻辑移动到 `PlaybackService`

**验证**：拔出蓝牙耳机，播放暂停

---

## 阶段 3：简化 AudioPlayerPlugin

### 3.1 重构 AudioPlayerPlugin.kt
核心改动：
- 移除 ExoPlayer 实例管理
- 移除 AudioDeviceCallback
- 通过 MediaController 连接 PlaybackService
- 保留: prepareArtworkDataUrl, cacheRemoteCover, getCachedWebDavAudioFile, prefetchWebDavAudioFile

**关键方法**：
```kotlin
@PluginMethod
fun load(call: PluginCall) {
    val uri = call.getString("uri") ?: return call.reject("missingUri")
    val songId = call.getString("songId") ?: return call.reject("missingSongId")
    val volume = call.getDouble("volume", 1.0) ?: 1.0
    val headers = call.getObject("audioHeaders")

    bridge.activity.runOnUiThread {
        // 构建 MediaItem
        val mediaItem = MediaItem.Builder()
            .setMediaId(songId)
            .setUri(Uri.parse(uri))
            .build()

        // 设置 Header（WebDAV 认证）
        // ...

        // 通过 MediaController 控制
        mediaController?.apply {
            setMediaItem(mediaItem, 0)
            prepare()
            playWhenReady = true
            setVolume(volume.toFloat().coerceIn(0f, 1f))
        }

        call.resolve()
    }
}

@PluginMethod
fun play(call: PluginCall) {
    bridge.activity.runOnUiThread {
        mediaController?.play()
        call.resolve()
    }
}

@PluginMethod
fun pause(call: PluginCall) {
    bridge.activity.runOnUiThread {
        mediaController?.pause()
        call.resolve()
    }
}

// ... 其他方法
```

**验证**：JS 可调用 load/play/pause，ExoPlayer 响应

---

## 阶段 4：重写 JS 层

### 4.1 重写 native.ts
- 移除 MediaSession 相关代码
- 简化状态同步逻辑
- 保留: prepareArtworkDataUrl, cacheRemoteCover, getCachedWebDavAudioFile, prefetchWebDavAudioFile

### 4.2 移除 mediaSession.ts
- 删除 `mediaSession.ts` 文件
- 从 `controller.ts` 移除 MediaSession 相关调用

**验证**：播放/暂停/切歌正常

---

## 阶段 5：集成测试

### 5.1 构建验证
- [ ] TypeScript 编译通过
- [ ] Android 编译通过
- [ ] 单元测试通过
- [ ] Lint 通过

### 5.2 功能验证
- [ ] 通知栏显示歌曲信息
- [ ] 通知栏控制按钮可用
- [ ] 锁屏控制可用
- [ ] 耳机按键可控制播放
- [ ] 后台播放正常
- [ ] 本地文件播放正常
- [ ] WebDAV 文件播放正常

---

## 阶段 6：清理与提交

### 6.1 移除遗留代码
- [ ] 删除 `capgo` 相关 import
- [ ] 删除 `mediaSession.ts`
- [ ] 清理 `controller.ts` 中的 MediaSession 调用

### 6.2 提交
```bash
git add -A
git commit -m "refactor(player): 用 ExoPlayer MediaSession 替换 capgo media-session

- 新增 PlaybackService 使用 MediaSessionService
- 简化 AudioPlayerPlugin 为 MediaController 桥接
- 移除 mediaSession.ts
- 移除 @capgo/capacitor-media-session 依赖"
```

---

## 回滚方案

如果 MediaSessionService 不稳定：

1. **快速回滚**：
```bash
git revert HEAD
pnpm add @capgo/capacitor-media-session@8.0.29
```

2. **恢复文件**：
   - `AudioPlayerPlugin.kt` → 从 git history 恢复
   - `mediaSession.ts` → 从 git history 恢复

---

## 预估工时

| 阶段 | 工时 | 风险 |
|------|------|------|
| 阶段 1：依赖配置 | 0.5h | 低 |
| 阶段 2：创建 PlaybackService | 2h | 中 |
| 阶段 3：简化 AudioPlayerPlugin | 2h | 中 |
| 阶段 4：重写 JS 层 | 2h | 中 |
| 阶段 5：集成测试 | 2h | 高 |
| 阶段 6：清理提交 | 0.5h | 低 |
| **总计** | **9h** | - |
