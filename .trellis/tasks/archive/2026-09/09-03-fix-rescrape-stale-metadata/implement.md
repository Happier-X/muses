# 实施计划：修复重新刮削后播放仍显示旧元信息

## 任务分解

- [ ] **T1 缓存失效能力** — `core:data/tag/AudioTagReader.kt` 新增 `invalidate(String)`，单测
- [ ] **T2 写回链路接失效** — `core:scrape/writeback/WritebackOrchestrator.kt` 注入失效依赖，文件成功后调用，单测
- [ ] **T3 懒扫描守卫** — `core:media/playback/PlaybackService.kt` 懒扫描分支按 `metaSources` 逐字段守卫，单测
- [ ] **T4 UI 优先级** — `app/navigation/MusesApp.kt` + `feature/library/SongsPage.kt` + `feature/player/PlayerViewModel.kt` 按字段优先库值，手测+单测
- [ ] **T5 回归与验收** — 全量 AC 手测与自动化单测回归

## 依赖顺序

T1 → T2 → T3 → T4 → T5。T3 依赖 T1 完成但可并行开发（mock invalidate）。

## T1 缓存失效能力

**改动文件**：`core/data/src/main/kotlin/com/muses/player/core/data/tag/AudioTagReader.kt`

**步骤**：
1. 新增 `fun invalidate(source: String)`：`tagCache.remove(source)` + 按 `getCacheFile` / `content_*` 规则删磁盘文件
2. 对 `content://` 场景匹配 `content_${hash}_` 前缀删除
3. 新增单测 `AudioTagReaderInvalidateTest`：写入缓存→invalidate→断言 cache miss / 文件不存在
4. 验证：`./gradlew :core:data:testDebugUnitTest`

**回滚点**：删除新增方法即回滚

## T2 写回链路接失效

**改动文件**：`core/scrape/src/main/kotlin/com/muses/player/core/scrape/writeback/WritebackOrchestrator.kt`、`core/scrape/src/main/kotlin/com/muses/player/core/scrape/di/ScrapeModule.kt`

**步骤**：
1. `WritebackOrchestrator` 构造新增参数 `audioTagCacheInvalidator: ((String) -> Unit)? = null`（函数式避免循环依赖 `core:data → core:scrape`）
2. `writeOne` 内 `fileResult.ok == true` 时 `try { invalidator?.invoke(song.path) } catch(_:Exception){}`
3. `ScrapeModule` 提供 `AudioTagReader` 实例并注入 lambda `reader::invalidate`
4. 单测 `WritebackOrchestratorTest` 新增：mock invalidator，断言成功分支调用一次、失败分支不调用
5. 验证：`./gradlew :core:scrape:testDebugUnitTest`

**回滚点**：移除构造参数与调用点

## T3 懒扫描守卫

**改动文件**：`core/media/src/main/kotlin/com/muses/player/core/media/playback/PlaybackService.kt`

**步骤**：
1. 读取 `entity.toDomain().metaSources` 暂存 `ms`
2. `tagData` 非空时，逐字段 `shouldOverride = ms?.field == null` 判定
3. `domain.copy` 时已刮削字段保留 `entity` 原值，未刮削字段才取 `tagData`
4. `tagsVersion` 仅当存在实际覆盖字段时置 1，否则保持原值（保守策略）
5. 单测：在 `androidTest` 或抽 `PlaybackLazyScanGuardTest`（纯逻辑抽函数可单测），覆盖：SCRAPE 标记不覆盖 / 无标记正常覆盖 / 内容为空不覆盖
6. 验证：`./gradlew :core:media:testDebugUnitTest :app:assembleMusesDebug`

**回滚点**：恢复无守卫的旧 copy 逻辑

## T4 UI 优先级

**改动文件**：
- `app/src/main/kotlin/com/muses/player/navigation/MusesApp.kt`（`MainViewModel.nowPlaying`）
- `feature/library/src/main/kotlin/com/muses/player/feature/library/SongsPage.kt`
- `feature/player/src/main/kotlin/com/muses/player/feature/player/PlayerViewModel.kt`（`refreshLyricsWithEntity` 封面分支）

**步骤**：
1. `MusesApp` 抽 `resolveDisplayField(songEntity, mediaMetadata)`：`metaSources.field != null → 用 song 值`
2. `SongsPage` 同款抽局部函数复用
3. `PlayerViewModel` 封面粘性分支增加 `metaSources.cover` 判断
4. 验证：手测 WebDAV/本地 刮削→播放→迷你条/列表/沉浸页标题一致为新值
5. 验证：`./gradlew :app:lintMusesDebug`

**回滚点**：恢复 `useMeta = tagsVersion < TAGS_VERSION` 旧分支

## T5 回归与验收

**验证命令**：
```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:data:testDebugUnitTest :core:scrape:testDebugUnitTest :core:media:testDebugUnitTest :app:lintMusesDebug
```

**手测清单**：
- WebDAV 刮削 `FILE_FAILED` 歌曲 → 播放 → AC1/AC2a/AC2b
- 本地刮削含封面成功 → `AudioTagReader` 读新封面 → AC3
- 未刮削 `tagsVersion=0` 首次播放懒扫描仍补齐 → AC5

## 风险与缓解

- `core:scrape` 依赖 `core:data` 导致循环：T2 采用函数式 `((String)->Unit)?` 而非直接依赖 `AudioTagReader`
- `PlaybackService` 单测需 Android 桩：逻辑抽纯函数 `shouldOverrideField` 置 `core:media` 可单测模块
