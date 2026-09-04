# P2c 执行计划

## 有序清单

- [x] S0 spike：ktor-bom 3.5.2 + CIO + 手写 multistatus 解析（xmlutil 未经验证弃用）；结论见 PRD 偏离 D5。
- [x] S1 包装器：LyricsHttp/ScrapeHttp Ktor 重写进 commonMain（API 冻结，429 平移，去 java.time）。
- [x] S2 provider 留守（D2，JVM 绑定硬证据），零改动；3 测试改 MockEngine。
- [x] S3 WebDavClient 留守做 Ktor 原地替换（D1/D3）；AuthRegistry 去 okhttp3；cache 仅清 import；OkHttpWebDavClientTest→MockEngine 重写。
- [x] S4 AC1/AC2/AC4 静态通过 + 全量门禁 BUILD SUCCESSFUL；新增 XML 边缘矩阵单测（含数字实体还原）；lyrics/scrape okhttp 主依赖与三模块 mockwebserver 已删；MuMu 深链路（WebDAV 后端/在线源）待用户实测。

## 验证命令（一键）

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:allTests :app:assembleMusesDebug testDebugUnitTest
grep -rn "okhttp3" core/webdav/src core/lyrics/src core/scrape/src --include="*.kt" | grep -v "/build/"
```

## 风险文件

- 两包装器（契约冻结，逐行对照）。
- `WebDavClient.parsePropfindResponse`（XML 语义，xmlutil spike 先行）。
- `WebDavAudioCache` 调用点（留守文件被迫联动，diff 注明）。
