# 实施计划：刮削写回失败排查与修复

## 前置检查

- 分支：`main`，已提交 `31b5cdb5`/`50054ed6`，当前 `git status` 仅 `09-02-scrape-writeback-fail` 未跟踪
- 依赖：`WebDavLibraryScanner`、`WebDavClient`、`TagWriter`、`ScrapeModule` 已就绪

## 步骤清单

### 1. 诊断加固（已完成，保留验证）
- [x] 1.1 `WritebackOrchestrator.writeOne` 增加 `Log.w("Writeback", "writeOne ... code/message")`
- [x] 1.2 `WebDavAudioTagFileWriter` 全链路 `Log.w/e`（`WebDavWrite`：`source/path/url`、`download ok/size`、`tagWrite ok/code`、`put ok`、`createTempFile` 异常）
- [x] 1.3 `Result` 页对 `FILE_FAILED` 增显 `fileResult.code:message`（`ScrapeScreen.kt`）
- 验证：`adb logcat --pid=$(pidof) -d | grep -E "Writeback|WebDavWrite"` 出现详细链路

### 2. WebDAV URL 404 修复
- [x] 2.1 `SongFileWriters.webDavWrite` 兼容完整 URL：`serverUrl` 前缀抽取后缀重编码，`http(s)://` 按自身 `scheme+host` 重建，否则走相对路径
- [ ] 2.2 回归：MuMu 上对 `bf83c4b6`（`https://openlist.happierx.xyz/dav/夸克网盘/.../0321 - space x.mp3`）重试，日志 `url=https://.../dav/%E5%A4%B8%E5%85%8B.../0321%20-%20space%20x.mp3` 且 `download ok`
- 验证：`./gradlew :core:scrape:test`（如存在）+ MuMu 手工重试

### 3. 临时文件扩展名修复
- [x] 3.1 从 `song.path` 提取真实后缀（截 `?`/`#`，长度≤5 字母数字），`createTempFile` 传入该后缀
- [ ] 3.2 回归：`bf83c4b6` 重试后不再出现 `No Reader associated with this extension:tmp`，`tagWrite ok=true`

### 4. 临时目录与 UI 遮挡修复
- [x] 4.1 `tempDir` 每次写入前 `mkdirs`，`createTempFile` 包异常返回 `download_failed`
- [x] 4.2 `ScrapeScreen` 三处底部 `Row` 增加 `navigationBarsPadding().padding(bottom=80dp)`
- [ ] 4.3 回归：`Queue/Preview/Result` 底部按钮在 `MiniPlayer` 之上完全可见；清缓存后写回仍成功

### 5. 综合验证
- [ ] 5.1 `./gradlew :feature:scrape:compileDebugKotlin :core:scrape:test` 通过
- [ ] 5.2 `./gradlew :app:assembleMusesDebug` 通过并在 MuMu 上 `install -r` 验证 `bf83c4b6` 单首与批量（≥2 首）写回均为 `成功`
- [ ] 5.3 撤销：`Result` 页“撤销上次”后曲库恢复

## 验证命令

```bash
./gradlew :feature:scrape:compileDebugKotlin
./gradlew :core:scrape:test  # 若无测试则跳过
./gradlew :app:assembleMusesDebug
adb -s emulator-5556 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk
adb -s emulator-5556 logcat --pid=$(adb -s emulator-5556 shell pidof com.muses.player) -d | grep -E "Writeback|WebDavWrite"
```

## 回滚点

- 任一步骤编译失败：`git diff` 回退对应文件，`./gradlew` 重跑
- MuMu 回归失败：`git log --oneline -1` 确认 `50054ed6` 后增量，`git revert` 或 `checkout HEAD -- <file>` 回退
- 风险文件：`SongFileWriters.kt`（URL 与临时文件）、`WritebackOrchestrator.kt`（日志）、`ScrapeScreen.kt`（UI）、`WebDavLibraryScanner.kt`（未改，仅观察）

## 产出

- 代码：`SongFileWriters.kt`、`WritebackOrchestrator.kt`、`ScrapeScreen.kt`
- 产物：MuMu 验证截图（`file-failed` 详情 → `成功 1`）
