# 实施计划：全量依赖升级

## 前置

- [x] 任务已 `task.py create`，`prd/design` 就绪
- [ ] `task.py start` 切至 `in_progress` 后由 `trellis-implement` 子代理执行（本会话为 inline 模式，主会话直接执行）

## 步骤

1. **探测最新版**：`web_search` 批量查询 `Kotlin`、`AGP`、`KSP`、`Hilt`、`Compose BOM`、`Media3`、`Room`、`Coil`、`OkHttp`、`Lifecycle`、`WorkManager` 的 `mvnrepository` 最新稳定版，记录于 `design.md` 版本映射表
2. **更新 `libs.versions.toml`**：按探测结果批量替换 `versions`，`Haze` 保持 `alpha03`，`accompanistLyricsCore` / `jaudiotagger` 等小众库保持或微升
3. **编译验证**：依次执行
   ```
   ./gradlew :core:ui:compileDebugKotlin
   ./gradlew :feature:library:compileDebugKotlin :feature:playlist:compileDebugKotlin :feature:sources:compileDebugKotlin :feature:scrape:compileDebugKotlin
   ./gradlew :app:assembleMusesDebug
   ```
   任一步失败则回退该依赖的版本并在 `design.md` 标注 `hold`
4. **冒烟**：`adb -s 127.0.0.1:7555 install -r app/build/outputs/apk/muses/debug/app-muses-debug.apk && am start`
5. **提交**：单 commit `chore(deps): bump libs.versions.toml to latest 2026-09-01`

## 回滚点

- `git checkout -- gradle/libs.versions.toml` 可一键回滚
- 若 `KSP` 大版本不兼容，优先回退 `Kotlin` 至 `2.4.10` 而非强升 `KSP`

## 产物

- 更新后的 `gradle/libs.versions.toml`
- `BUILD SUCCESSFUL` 日志与 `MuMu` 启动截图（可选）
