# Android 原生开发规范

> 适用于仓库根纯原生 Kotlin + Jetpack Compose 工程。
> 纯原生重写已完成（Capacitor/Web 层已删除）；仓库结构已扁平化（无 native/ 前缀）。

## 特征规范索引

- [features-lyrics-playlist.md](features-lyrics-playlist.md) — AMLL WebView 歌词渲染 / 播放列表 / 响度均衡（M2）
- [features-scrape-engine.md](features-scrape-engine.md) — 刮削引擎：数据层 + UI 接线（刮削页四态/云编辑/自动补缺）（M3）
- **M4 平板双栏**：≥768dp TabletLayout 契约见 features-salt-ui.md 陷阱 #16
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
- WebDAV 曲目：直接 HTTP URL 流播（ExoPlayer），数据源经 **CacheDataSource 边播边缓存**——
  探测性重复 Range 请求命中本地不再发网络（防网关限流）；详见 features-webdav-library.md
- 沉浸式播放页为**纯原生 Compose**（P4.4 后 WebView 已废弃，详见 features-lyrics-playlist.md §7）：`PlayerScreen` + `MeloXFlowingLightBackdrop`（流体 Blob + 封面虚化）+ `MeloXIOSLyricsPanel`（逐词高亮）+ `PlayerViewModel` 粘性封面/歌词解析链路；布局 1:1 复刻 Capacitor `PlayerPage.vue` 的 BEM 结构（drag-layer / bg / song-head-fixed / panels 200% / cover-hero / meta-window / bottom-bar）
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

- 完整契约（扫描/缓存/凭据注册表/限流教训/懒扫描）见 [features-webdav-library.md](features-webdav-library.md)
- `OkHttpWebDavClient`：PROPFIND XML 用 XmlPullParser 解析（不用 DOM）；Basic 编码显式 UTF-8
- `WebDavAudioCache`：LRU 500MB 上限，`.meta` 与 cache 同名前缀关联（勿二次哈希）
- 密码仅在内存中持有，不持久化到网络层

## 错误日志设施（任务 08-26-settings-log-viewer）

- `ErrorLogStore`（core:data/log）：接口 + `@Binds @Singleton` 绑定在 `RepositoryModule`；
  仅收 WARN/ERROR 到内存环形缓冲（ArrayDeque cap=500，synchronized）
- **埋点规则**：新代码的静默 catch 必须补 `errorLogStore.log(ERROR, tag, msg, e)`；
  挂起调用外包 catch 时前置 `catch (e: CancellationException) { throw e }`（不计日志、原样重抛）
- `CrashHandler`：`MusesApplication.onCreate` 安装（super.onCreate 之后）；install 全程 try-catch
  不抛异常；崩溃时写 `filesDir/error_log/crash-latest.txt` 后**必须委托原 handler**；启动读回后删文件
- 设置页「反馈」分组消费：副标题 `latestSummary`，点击复制 `dump()` 全文；
  同一实现双接口绑定（查询 `ErrorLogStore` / 序列化 `ErrorLogCrashPersistence`）避免 Application 注入实现类
- 新增埋点时勿扩 diff 半径顺手改其它静默 catch——逐任务增量接入

## 构建验证命令

```bash
# 仓库根执行；多 flavor 项目装包/编译验证一律 assembleMusesDebug（裸 assembleDebug 是无效旧 variant）
# lint 任务在 :app 上（root 非 Android 项目无 :lintDebug）：:app:lintMusesDebug
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
```

## 禁止模式

- 禁止 `feature:*` 直接 import `androidx.room` / `okhttp3`
- 禁止在 ViewModel 中持有 Context 引用
- 禁止在 Composable 中做网络/IO 操作
- 禁止硬编码字符串（应提取到 strings.xml，M1 可暂容忍）
