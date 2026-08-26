# 实现计划：设置页日志功能

## 前置

- [ ] 阅读 `.trellis/spec/android/index.md` 与 features-salt-ui.md（Salt 组件约定）

## 步骤（有序）

1. **core:data 日志设施**
   - [ ] 新建 `core/data/src/main/kotlin/com/muses/player/core/data/log/ErrorLogStore.kt`
         （接口 + Level 枚举 + latestSummary StateFlow + dump()）
   - [ ] 新建 `.../log/RingBufferErrorLogStore.kt`（ArrayDeque cap=500，synchronized；
         dump 格式见 design.md）
   - [ ] 新建 `.../log/CrashHandler.kt`（安装/持久化/启动读回，见 design.md 契约）
   - [ ] 在 `RepositoryModule` 加 `@Binds`
2. **Application 接线**
   - [ ] `MusesApplication.onCreate()` 首行调用 `CrashHandler.install(this)`——
         注意 Hilt 单例注入时机：install 用 Application context 直接构造，
         或通过 `EntryPointAccessors` 取 ErrorLogStore；二选一在实现时定
3. **关键埋点（R2）**
   - [ ] `PlaybackService.kt onPlayerError` 补 error 日志
   - [ ] `WebDavLibraryScanner.kt:56`、`ScanWorker.kt:58` 静默 catch 补 error 日志
4. **设置页 UI（R3）**
   - [ ] SettingsViewModel 注入 ErrorLogStore，暴露 latestSummary 与 copyLogs()
   - [ ] SettingsScreen 新增「反馈」分组 + SaltListItem + Toast 提示
5. **测试**
   - [ ] RingBufferErrorLogStore 单元测试：容量淘汰 / dump 格式 / latestSummary
   - [ ] CrashHandler 持久化-读回逻辑单测（Robolectric，tmp filesDir）

## 验证命令

```bash
# 仓库根执行；多 flavor 项目一律 assembleMusesDebug（裸 assembleDebug 是无效旧 variant）
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:data:testDebugUnitTest
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :lintDebug testDebugUnitTest
```

## 风险文件 / 回滚点

- `PlaybackService.kt` / `ScanWorker.kt`：播放与后台任务核心链路，埋点为单行 log 调用，
  出错即 revert 该行
- `MusesApplication.kt`：crash handler 安装必须不抛异常（try-catch 包裹 install 内部逻辑），
  否则会把正常启动变成崩溃
- 整体回滚：单 commit revert

## start 前检查

- [ ] implement.jsonl / check.jsonl 已含真实条目（非 _example 种子）
- [ ] 用户已批准最终规划摘要
