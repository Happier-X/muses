# core:scrape KMP 化执行计划

## 有序清单

- [ ] W1 仓库地基：SongRepository/SourceRepository/CredentialsRepository + ErrorLogStore 上收 `:core:common` commonMain；core:data 转发保安卓 Koin 绑定零改动；验证 `:core:common:assemble + jvmTest + :app:assembleMusesDebug`。
- [ ] W2 纯逻辑上收：text/cover provider 全家 + SuspiciousDetector + NegativeCache + EditCloudMetaSearch + ScrapeRateLimiter 收敛迁入 commonMain；core:scrape 同包名删除；双端编译。
- [ ] W3 写回链：TagPort/WebDavClient 接口上收；WritebackOrchestrator/SongFileWriters 迁 commonMain（经 Port）；jaudiotagger 标签实现双端 sourceSet；单测覆盖写回语义。
- [ ] W4 桌面装配：DesktopScrapeGraph 接真实引擎（桌面刮削页去回调占位，歌词维度明示降级）；MuMu/桌面实测（扫队列→匹配→预览→写回）；AC 全勾 + 全量回归 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:jvmTest :core:common:allTests :app:assembleMusesDebug testDebugUnitTest :composeApp:compileKotlinJvm
```

## 风险文件

- `core/data/repository/Repositories.kt`（三仓库上收，安卓双绑定转发）。
- `core/scrape/writeback/*`（写回语义冻结，TagPort 契约先行单测）。
- `core/webdav/WebDavClient.kt`（接口上收，实现留原地）。
- `DesktopScrapeGraph`（W4 接通真实引擎，DataStore 文件隔离教训沿用）。
