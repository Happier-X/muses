# KMP 迁移技术设计

> 基于当前版本线冻结评估：Kotlin 2.4.10 / AGP 9.3.2 / Media3 1.11 / Room 2.8.4 / Hilt 2.60 / OkHttp 5.5。桌面 MVP = 前台播放 only（已决策，托盘/SMTC/全局媒体键放二期）。

## 1. 模块可迁移性分级

| 模块 | 现状依赖 | 等级 | 迁移方案 |
|---|---|---|---|
| `core:model` | 纯 JVM，零安卓依赖 | S（直迁） | 整体搬入 `commonMain`，零改动 |
| `core:lyrics` 在线搜索 | OkHttp + serialization + coroutines | A（换库） | OkHttp → Ktor-client；QRC 解密等纯逻辑直迁 |
| `core:scrape` | OkHttp + serialization + DataStore + 依赖 data/webdav/media/lyrics | A-（换库+解耦） | 先等 data/webdav 建好 Port 接口再迁；DataStore 用多平台版本 |
| `core:webdav` | OkHttp 手写 PROPFIND + XmlPullParser | A-（换库） | Ktor-client 重写传输层；XML 解析换 `xmlutil`（KMP）；`WebDavAudioCache` 文件 IO 用 okio（KMP） |
| `core:data` | Room + DataStore + Keystore + WorkManager | B（换驱动） | Room 2.7+ KMP（换 `SQLiteDriver` 平台实现，schema/migration 测试重写）；DataStore 多平台；凭据改 `expect/actual`（Android Keystore / Windows DPAPI 或 Credential Manager）；WorkManager 后台任务桌面侧用协程调度替代 |
| `core:ui` + `feature:*` | Compose M3 + Coil3 + Haze + Tabler cmp-android | B+（CMP 化） | CMP 天然支持；Coil3 已是 KMP；Haze 桌面效果降级验证；图标已是 CMP 变体；`hiltViewModel` → Koin `koinViewModel` |
| `core:media` 播放 | Media3 全家桶 + MediaSessionService + CacheDataSource | C（重写桌面侧） | 见 §2；安卓侧保持 Media3 不动 |

## 2. 播放器抽象（最贵的部分，MVP 范围内）

现有接缝（见 `.trellis/spec/android/index.md` 播放契约）：UI/ViewModel 只经 `PlaybackController` 接口 + `PlayerConnection` 的 StateFlow 驱动 `PlaybackService`。迁移时把该接口下沉为 `commonMain` 的 `PlayerPort`：

```
commonMain: PlayerPort（play/pause/seek/enqueue/setMode + playbackState/error StateFlow）
  ├─ androidMain: Media3PlayerPort（复用现有 PlaybackService/PlayerConnection，几乎零改动）
  └─ desktopMain: JvmPlayerPort（新写：VLCJ 或 javax.sound 解码 + 本地队列状态机复刻 repeat/shuffle 语义）
```

- 播放持久化（`PlaybackStateRepository` 快照 key `playback_snapshot`）与失败恢复链（`PlaybackRecoveryController` 回绕跳过）是纯逻辑，可进 `commonMain`，两端复用。
- WebDAV 边播边缓存（CacheDataSource）是 Media3 专属：桌面侧用 Ktor Range 下载 + okio 本地文件 spiller 自实现，不追求首版对等。
- 音频焦点/SMTC/托盘明确二期，首版桌面 `JvmPlayerPort` 不实现（接口预留 `actual` 空实现 + TODO 标记）。

## 3. DI 替换策略（Hilt → Koin）

- Hilt（`@HiltAndroidApp/@HiltViewModel/@Singleton/@Binds`）不支持 KMP，必须全量换 Koin（约 43 个文件，见评估 grep）。
- 策略：先在安卓侧把 Hilt 换 Koin并全量回归（风险隔离：纯安卓、可单步验证），再建 KMP 模块时 DI 层直接复用。禁止 Hilt/Koin 双轨长期并存。
- `ErrorLogStore` 的“同一实现双接口绑定”等 Hilt 技巧换 Koin `singleOf/bind` 等价表达；`HiltWorker` 换普通协程调度。

## 4. 数据层 KMP 方案

- Room KMP：entities/dao 升为 `commonMain`（SQL 方言以现有 schema 为准），`MusesDatabase` 配 `AndroidSQLiteDriver` / `JvmSQLiteDriver`（bundled-sqlite）；`schemas/` JSON 与 Migration 测试按 Room-KMP 布局重写后保留。
- DataStore Preferences 已有多平台实现，key 名（`playback_snapshot` 等）冻结不变，保证双端行为一致。
- 文件系统统一用 okio `FileSystem` 抽象（`WebDavAudioCache` 500MB LRU、crash 日志 `filesDir/error_log` 均收敛到此）。

## 5. 渐进式路线（四阶段，禁止一次性翻转）

1. **P0 清理**（子任务，已立项）：删 webkit 依赖/注释/`_lyricsJson` 死代码，验证 `:app:assembleMusesDebug`。
2. **P1 common 先行**：新建 KMP 模块，搬 `core:model` + lyrics/scrape 纯逻辑 + `PlayerPort` 接口定义；安卓侧依赖验证，行为零变化。
3. **P2 数据层**：Room/DataStore KMP 化 + webdav 切 Ktor + Hilt→Koin（安卓侧先行）；本阶段结束安卓包必须全量回归通过。
4. **P3 桌面壳**：`composeApp(desktop)` 最小可用（库房/播放/设置），`JvmPlayerPort` 前台播放；歌词特效（Haze/Blur）桌面降级；托盘/SMTC 二期。

## 6. 备选对比

| 方案 | 成本 | 双端一致性 | 结论 |
|---|---|---|---|
| 全量 KMP+CMP（本设计） | 高（C 级播放重写 + 43 文件 DI） | 最高 | 长期最优，但首版桌面交付慢 |
| 安卓不动 + 桌面精简 JVM 客户端（只复用 model/scrape/lyrics 协议） | 低（2-4 周量级） | UI 各写各的 | 想“先有个桌面能用”选它；且与 P1/P2 兼容，进可攻 |

## 7. 主要风险

- 桌面解码选型（VLCJ 体积大但格式全 vs javax.sound 轻但格式少）需原型验证后再定，未定前 P3 不开工。
- Room-KMP 的 migration 测试链与现有 `schemas/` 兼容性需 spike 确认。
- `XmlPullParser` 的 KMP 替代（xmlutil）在大 PROPFIND 响应下的性能未经验证。
- 团队 CMP 桌面打包/签名/分发链（msi/exe）从零搭建。
