# core:lyrics KMP 化技术设计

## 1. 目标形态

```text
:core:common
  ├─ commonMain/lyrics/     # 既有：LyricsHttp / LyricsProviderUtil / QrcTables（P2c）
  └─ jvmShared/lyrics/      # 新迁：parser/provider/client/crypto/matcher/amll/lrclib/aligner/model/store
                            # （javax.crypto/javax.xml/java.* 零改动；双 target 均 JVM 系）
:core:lyrics 瘦壳
  └─ di/LyricsModule.kt（koin-core DSL，可上收或留守，视绑定面）
桌面：DesktopScrapeGraph lyricsPorts 接通（AmllLyricsPort + PlatformChain + Lrclib）
```

## 2. 关键处置

| 项 | 处置 |
|---|---|
| javax.crypto（WyCrypto/Qrc 加密） | jvmShared 零改动（双端 JVM） |
| javax.xml（TtmlLyricsParser DOM） | jvmShared 零改动 |
| java.text/util（Normalizer 等） | jvmShared 零改动（core:scrape 的 expect 化是因要进 commonMain，本模块不需要） |
| LyricBindingStore（Context+org.json） | 构造收 `File`/路径（调用方注入），org.json→kotlinx.serialization `Json.parseToJsonElement`/`encodeToJsonElement`；行为冻结（绑定 key/存储格式不变） |
| LyricsModule（DI） | koin-core DSL 无安卓依赖；随迁 commonMain/jvmShared 或留 core:lyrics 壳，以安卓 Koin 装配零改动为准 |
| 测试（core/lyrics/src/test） | 迁 core:common jvmTest（kotlin.test 或留 JUnit4——jvmTest 可用 JUnit4，零改动优先） |

## 3. 迁移顺序（X 系列）

X1 LyricBindingStore 去安卓化 → X2 主源码全量迁 jvmShared + 测试随迁 → X3 core:lyrics 瘦壳清空 → X4 桌面刮削歌词维度接通 + 回归。

## 4. 风险与回滚

- org.json→kotlinx.serialization 的解析宽容度差异（JSON 格式存储），迁移前先写冻结用例。
- jvmShared 中间层与 commonMain 同包名共存（LyricsHttp 在 commonMain）：jvmShared dependsOn commonMain，同包名不同文件合法，编译实证即可。
- 每阶段独立提交可 revert；安卓门禁全程。
