# Android 原生开发规范

> 适用于 `native/` 纯原生 Kotlin + Jetpack Compose 工程。

## 特征规范索引

- [features-lyrics-playlist.md](features-lyrics-playlist.md) — AMLL WebView 歌词渲染 / 播放列表 / 响度均衡（M2）
- [features-scrape-engine.md](features-scrape-engine.md) — 刮削引擎数据层：五源匹配链 / 写回编排 / 回滚 journal（M3）
- [features-lyrics-online.md](features-lyrics-online.md) — 歌词在线搜索：五源+LRCLIB+AMLL TTML / QRC 解密 / 编排链

- [features-salt-ui.md](features-salt-ui.md) — Salt UI 组件体系：m-* 映射/设计令牌/布局陷阱（08-25-native-salt-ui）
- [features-webdav-library.md](features-webdav-library.md) — WebDAV 曲库链路：扫描/播放整文件入缓存/凭据注册表/限流教训（08-25-webdav-source-scan）

---

## 架构分层

```
app (UI 宿主、导航、引导) 
  → feature:* (页面 UI + ViewModel)
    → core:* (业务逻辑/数据/网络)
      → core:model (纯 Kotlin 领域模型)
```

- `feature:*` **禁止**直接依赖 Room、OkHttp、Media3 等实现库，只依赖 `core:*` 的接口与 `core:model`
- 所有跨模块通信通过接口 + Hilt `@Binds` 实现

## 技术栈

| 项 | 选型 |
|---|---|
| Kotlin | 2.x（Version Catalog 管理） |
| Compose | BOM + Material 3 基础组件 |
| DI | Hilt（@HiltAndroidApp / @HiltViewModel / @Singleton） |
| 异步 | Coroutines + Flow；UI 层 StateFlow + collectAsStateWithLifecycle |
| 持久化 | Room（库数据）+ DataStore Preferences（设置）+ Keystore（凭据） |
| 网络 | OkHttp 5 |
| 播放 | Media3 ExoPlayer + MediaSessionService |
| 图片 | Coil 3（AsyncImage） |
| 导航 | navigation-compose |

## Salt 主题层

- `SaltTheme` 挂载于 `MaterialTheme` 之上，通过 `CompositionLocal` 暴露玻璃梯度等专属颜色
- `GlassSurface` 组件：`Modifier.blur()` + 半透明 surface，播放页独占全屏模糊
- 列表页禁用全屏模糊（性能预算）

## 播放契约

- `PlaybackService : MediaSessionService` 负责播放 + 通知 + 媒体按钮
- `PlayerConnection` 封装 MediaController，暴露 StateFlow 给 ViewModel
- WebDAV 曲目：检查 `WebDavAudioCache.getCachedFile()`；命中用 file://，否则 OkHttp 流播
- 音频焦点由 ExoPlayer 默认处理（handleAudioFocus=true）
- `onTaskRemoved` → `stopSelf()`（后台播放安全）

## 播放持久化契约（任务 08-25-native-playback-persistence）

- `PlaybackStateRepository`（core:data）：队列快照+会话合并 key `playback_snapshot`、配置 key `playback_config`；JSON schema 带 version，宽松解析回退默认值
- 默认值：repeat=all / shuffle=false / loudnessNormalize=true（仅显式 false 关闭）
- `PlaybackService`：onCreate 恢复队列+seekTo（不自动播放）；转场/暂停/seek 500ms debounce 保存；onDestroy runBlocking(2s 超时) 强制落盘
- `RecentPlaysRepository`：同曲去重置顶、上限 50，MEDIA_ITEM_TRANSITION 时登记
- 队列操作算法不移植（Media3 shuffle/repeat 承担）；shuffleOrder 恢复时按开关重洗

## 播放失败恢复链（任务 08-25-native-playback-recovery）

- `PlaybackService` 的 persistenceListener.onPlayerError：登记失败曲 → `PlaybackRecoveryController.selectNextCandidate` 沿 active order 回绕一次跳过 attempted → seekTo+prepare+play；无候选才停止
- 安全文案白名单 8 条（`PlaybackErrorCopy`）：IO_FILE_NOT_FOUND→文件失效、网络类→检查网络、AUTH_EXPIRED/BAD_HTTP→WebDAV 认证失败；未知统一「播放失败，请稍后重试。」不泄露内部信息
- `PlayerConnection.playbackError: StateFlow<String?>` 供播放页消费；用户主动 play() 重置恢复链与错误
- #53 resume-seek-guard 不移植：`setMediaItems(items,index,pos)` 原子定位天然无中间态

## WebDAV 客户端

- `OkHttpWebDavClient`：PROPFIND XML 用 XmlPullParser 解析（不用 DOM）
- `WebDavAudioCache`：LRU 500MB 上限，`.meta` 文件记录 eTag/lastModified/lastAccess
- 密码仅在内存中持有，不持久化到网络层

## 构建验证命令

```bash
cd native && JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug :lintDebug testDebugUnitTest
```

## 禁止模式

- 禁止 `feature:*` 直接 import `androidx.room` / `okhttp3`
- 禁止在 ViewModel 中持有 Context 引用
- 禁止在 Composable 中做网络/IO 操作
- 禁止硬编码字符串（应提取到 strings.xml，M1 可暂容忍）
