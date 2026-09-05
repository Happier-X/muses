# core:scrape KMP 化技术设计

## 1. 目标形态

```text
:core:common（KMP，android+jvm 双 target）
  └─ commonMain/scrape/            # 刮削引擎全量（纯逻辑 + Port 接口）
      ├─ text/   provider×5 + Matcher/Confidence/Util/NegativeCache
      ├─ cover/  CoverMatch + provider×6
      ├─ writeback/  WritebackOrchestrator + SongFileWriters + TagPort
      ├─ editmeta/   EditCloudMetaSearch + LyricsSearchPort（接口）
      ├─ queue/    SuspiciousDetector
      └─ ports/    ScrapeReposPort / WebDavClient 接口 / TagPort
  └─ jvmMain & androidMain：jaudiotagger 标签实现（同库双端）
:core:scrape（安卓库瘦壳）
  └─ ScrapeModule（Koin 安卓装配：注入 core:common 引擎 + 安卓 Port 实现）
:core:common 安卓侧 Port 实现：三仓库（commonMain 实体直用）+ AudioTagReader 壳（androidMain）
桌面：DesktopScrapeGraph 注入 commonMain 引擎 + 桌面 Port 实现
```

## 2. 依赖处置映射

| 现 core:scrape 依赖 | 处置 |
|---|---|
| core:data 三仓库（220 行，纯 common 依赖） | 移入 commonMain；core:data 保留 typealias/转发（安卓 Koin 绑定不变） |
| core:data ErrorLogStore | 确认位置；如安卓专属则随仓库一起上收（实现为内存环形缓冲，零平台依赖） |
| core:data AudioTagReader（Context/Uri/OkHttp 壳） | 拆 TagPort 接口（commonMain）+ 安卓壳留 androidMain + jaudiotagger 核心双端共用 |
| core:media TagWriter（jaudiotagger 纯 JVM） | 实现 jaudiotagger 部分双端共用；经 TagPort 注入 |
| core:webdav WebDavClient 接口 | 接口移 commonMain（依赖 ErrorLogStore + Ktor，均 commonMain 可用）；KtorWebDavClient 实现留 core:webdav；桌面按同契约实现或复用 |
| core:lyrics（LyricsMatcher/Lrclib/Amll/PlatformChain） | 不动（D1）；LyricsSearchPort 接口留 commonMain，安卓注入实现，桌面不注入（D2 降级） |

## 3. Port 契约（commonMain 只定接口）

- `ScrapeReposPort`：songRepo/sourceRepo/credentialsRepo 三访问器（直接持 commonMain 仓库实例，或惰性提供者）。
- `TagPort`：readTags(file) / writeTags(file, changes)（jaudiotagger 双端实现）。
- `WebDavClient`：接口原样上收（authenticate/probe/list/get/put/delete/move/getString，File→Path 已在 P2c 完成）。
- `LyricsSearchPort`：沿用 editmeta 现有接口原样上收。

## 4. 阶段（W 系列见 implement.md）

W1 仓库地基（三仓库+ErrorLogStore 上收，安卓转发）→ W2 纯逻辑上收（text/cover/queue/editmeta 搜索）→ W3 写回链（Port 化 + TagPort 双端实现）→ W4 桌面装配联调 + 回归。

## 5. 风险与回滚

- 最大未知：jaudiotagger 在 JVM 桌面的写回行为与安卓一致性（同一纯 Java 库，预期一致；W3 先单测验证）。
- core:data 三仓库搬迁涉及安卓 Koin 绑定（双绑定模式），转发层保零改动。
- 每阶段独立提交可 revert；安卓行为门禁 `:app:assembleMusesDebug + testDebugUnitTest` 全程。
