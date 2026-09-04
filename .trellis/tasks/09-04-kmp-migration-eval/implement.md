# KMP 迁移执行计划

> 本文件是迁移纲领的阶段清单。P0（残留清理）是批准后可立即执行的交付；P1–P3 需另行立项（本任务归档后）。

## 阶段 0：WebView 残留清理（主任务内执行，对应 PRD-R4）

- [x] R1：删 `feature/player/build.gradle.kts:61` webkit 依赖（含 :60 注释）
- [x] R2：删 `gradle/libs.versions.toml` 的 `androidxWebkit` 版本行 + `androidx-webkit` 库条目
- [x] R3：改写 `PlayerScreen.kt:215/249`、`PlayerViewModel.kt:98/139/238` 注释去 WebView 化
- [x] R4：删 `_lyricsJson` / `lyricsJson` 及 `refreshTranslationState` 内赋值分支（附带删只写变量 `currentSongId` 字段+赋值）
- [x] 验证：AC1/AC2（SaltNavbar.kt:45 为 R5 豁免）通过；`:app:assembleMusesDebug` 通过；`:feature:player:testDebugUnitTest` 无源码通过
  ```bash
  grep -rn -i "webkit" gradle/ app/build.gradle.kts feature/*/build.gradle.kts core/*/build.gradle.kts
  grep -rn "WebView" app/src core/*/src feature/*/src --include="*.kt" | grep -v SaltNavbar.kt  # SaltNavbar.kt:45 为 R5 豁免的设计备注
  JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug
  ```
- 风险文件/回滚点：仅构建脚本与注释+死代码，回滚 = `git revert` 单提交；`PlayerViewModel.refreshTranslationState` 改后需确认 `lyricsJson` 无其他消费方（评估已确认 0 引用，执行时复核一次）。

## 阶段 1：common 先行（另行立项，预估 1–2 周）

- [ ] 新建 KMP 模块骨架（`commonMain/androidMain`），CI 加 `desktop` 编译任务
- [ ] 搬入 `core:model` 全量 + lyrics/scrape 纯逻辑 + 定义 `PlayerPort` 接口
- [ ] 安卓侧依赖切换，行为零变化验证：`testDebugUnitTest` 全过 + `:app:assembleMusesDebug`

## 阶段 2：数据层 + DI（另行立项，预估 3–5 周，风险最高）

- [ ] Hilt→Koin（安卓侧先行，约 43 文件），全量回归
- [ ] Room-KMP（先做 migration-schemas spike）+ DataStore 多平台 + okio FileSystem 收敛
- [ ] webdav 传输层切 Ktor-client，MockWebServer 测试平移
- [ ] 验证：`:app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest`（见 spec 构建命令）

## 阶段 3：桌面壳 MVP（另行立项，前置：解码选型原型结论）

- [ ] `JvmPlayerPort` 前台播放（VLCJ 或 javax.sound，原型定）
- [ ] `composeApp(desktop)`：库房/播放/设置最小可用；歌词特效桌面降级
- [ ] 桌面打包/签名/分发链（msi/exe）+ 首版发布
- [ ] 二期 backlog：托盘/SMTC/全局媒体键/音频焦点/CacheDataSource 对等

## 通用门禁

- 每阶段结束：全量编译 + 单元测试 + 当阶段产出物归档对应 Trellis 任务。
- 任何阶段发现工作量超预估 50% 即回父任务重估，禁止硬扛。
