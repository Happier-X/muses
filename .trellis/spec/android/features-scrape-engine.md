# 刮削与元数据引擎（M3 数据层）

> 任务 08-25-native-m3-scrape-engine。以 Web 层 `src/features/{scrape,metadata,cover,editMeta}` 源码为规格书翻译的纯 Kotlin 数据层，全部落在 core/scrape（+ core:model/core:data/core:media 增量）。UI 接线归后续任务。

## 模块与分层

```
core:scrape
├── text/      五源文本匹配链 kw→tx→wy→kg→mg（TextMetaMatcher）+ 置信度 + 负缓存
├── cover/     六源封面链 iTunes→kw→tx→wy→kg→mg（CoverMatcher）
├── writeback/ 写回编排五步 + 回滚 journal（DataStore）+ 失败文案映射
├── queue/     待刮削队列 / 历史（滚动 200）/ 可疑检测
├── editmeta/  编辑页强制云搜三维编排（文本/封面/歌词）
├── http/      ScrapeHttp：非 2xx 抛 IOException("http <code>") 不重试
└── di/        ScrapeModule（Hilt 装配，不接线 UI）
```

## 关键契约

- **Web=规格书**：算法常量逐一翻译并注释来源 .ts 文件（TTL 45min、负缓存容量 256、journal/历史上限 200、maxCandidates 默认 8）
- **存储替换**：localStorage → DataStore Preferences JSON snapshot（key：`scrape_rollback_journal` / `scrape_queue` / `scrape_history`），schema 带 version，解码宽松回退空表；编解码集中在 `writeback/WritebackJson.kt`（手工 JsonElement，未引 @Serializable 插件——AGP 9 内置 Kotlin 兼容性待验证）
- **写回五步**：快照→写文件（本地并行/WebDAV 串行）→写库（metaSources 标记 embedded/scrape）→逐行 success/file-failed/failed→撤销仅恢复库旧值
- **Room v4**：songs 表新增 `lyricsFormat/lyricsSource/metaTitle/metaArtist/metaAlbum/metaCover` 六列（MIGRATION_3_4）；Song 领域模型对应扩展
- **歌词维度边界**：editmeta 只做编排（去重 key=`source\1format\1text[0..120]`、ttml/yrc/qrc 优先粗排），具体歌词 provider 与 AMLL 聚合通过 `LyricsSearchPort` 注入，本任务不接线

## UI 接线契约（08-26-m3-scrape-metadata）

- **feature:scrape**：ScrapeScreen 四态机（queue/matching/preview/result）+ ScrapeViewModel 编排
  TextMetaMatcher+CoverMatcher+WritebackOrchestrator；ScrapeQueueAccessViewModel 供跨页面入队
- **写回安全红线**：预览候选 `checked = false` 默认全不选；「写回选中」按钮 enabled 绑定 any{checked}
- **歌曲页入口**：经 `onEnqueueScrape` 回调注入（feature:library 不直接依赖 core:scrape）；
  MultiselectBottomBar 与 ⋮ 菜单两处入口
- **EditMetaSheet 宿主在 MusesApp 层**：播放页 WebView「更多」键 → 桥动作 openEditMeta → 回调弹全局
  BottomSheet；song 为 null 时搜索/应用均 disabled
- **自动补缺**：`auto_scrape_enabled` DataStore 开关默认关；音源页扫描成功后
  `getUntaggedSongIds()`(tagsVersion<1) 入队。ScanWorker 路径因 core:scrape→core:media 循环依赖不接，
  只保留音源页扫描入口
- **LrclibProvider 需全局 Hilt 绑定**：LyricsModule @Provides（此前仅手动构造无绑定，UI 接线后暴露）
- **协程红线**：matcher/search 外包 catch 必须前置 rethrow CancellationException

## 已知缺口（接线 UI 前必须解决）

1. **WebDAV username 未持久化**：M1 Source 模型无 username 字段，`WebDavAudioTagFileWriter` 认证用户名暂传空串——需先补 username 存储
2. 刮削触发时机接线（入库后自动补缺调度）、ScrapePage 等页面归后续任务

## 踩坑记录

- `URLEncoder.encode(String, Charset)` 重载需 API 33（minSdk 26）：统一用 `text/provider/KwProvider.kt` 的 `urlEncode`（charset 名重载 + `+→%20` 对齐 encodeURIComponent）
- `ScrapeHttp.getJson` 返回 `JsonElement`（非 JsonObject）：取字段须先 `asObjectOrNull()` 或用 `path(...)` 下钻，不能直接 `[key]`
- itunes 封面放大正则带**前导斜杠** `/\d+x\d+([a-z]*)\./i`（漏掉会产生 `//600x600` 双斜杠）
- MockWebServer 测硬编码域名 provider：OkHttp interceptor 把 host 重写到 loopback（见 CoverProviderParseTest/TextMetaMatcherTest 的 httpFor）
