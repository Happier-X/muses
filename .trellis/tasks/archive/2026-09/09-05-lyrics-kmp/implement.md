# core:lyrics KMP 化执行计划

## 有序清单

- [x] X1 LyricBindingStore 去安卓化：Context→路径注入、org.json→kotlinx.serialization；冻结用例先行；验证 `:core:lyrics:testDebugUnitTest`。
- [x] X2 全量迁 jvmShared：主源码 30 文件 + 测试随迁 core:common jvmTest；消费者同包名零改动；验证三端编译 + 单测。
- [x] X3 core:lyrics 瘦壳：DI 装配处置（上收或留守）；build.gradle 清理。
- [x] X4 桌面接通：DesktopScrapeGraph lyricsPorts 注入（AmllLyricsPort/ProviderLyricsPort）；刮削页歌词降级文案移除；桌面在线歌词数据链最小可用；AC 全勾 + 全量回归 + 归档。

## 验证命令

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:allTests :app:assembleMusesDebug testDebugUnitTest :composeApp:compileKotlinJvm
```

## 风险文件

- `LyricBindingStore.kt`（org.json→kotlinx.serialization，存储格式冻结用例先行）。
- `WyCrypto.kt` / `QQMusicQrcLyricsParser.kt`（加密语义冻结，测试逐字平移）。
- `LyricsModule.kt`（Koin 装配零改动红线）。
