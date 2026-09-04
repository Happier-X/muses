# P3 桌面 MVP 执行计划

> 前置门禁：P2c 网络层切换停稳提交后，P3 实现才开工（同工作目录防冲突）。S0 原型可用独立目录先行。

## 有序清单

- [x] S0 解码原型（失败即停）：独立分支/目录验证 VLCJ 三项——FLAC 实播（含 24bit/高采样）、seek 精度（±1s）、干净 Windows 机跑通（原生库随包）；输出书面结论。任一不过 → 回退 JavaFX Media，重出结论。
  - 结论见 `spike.md`：三项全过，技术锁定 VLCJ；VLCJ 实测为 GPL 系（非 LGPL），S2 开工前需决策人确认分发合规。
- [x] S1 数据层桌面接线：`DataStorePath.jvm` 真实路径 + Room `JvmSQLiteDriver` + 凭据 expect/actual（DPAPI/Credential Manager）；验证 `:core:common:compileKotlinJvm + jvmTest`。
  - 已交付：PlatformDirs/PlatformCryptoEngine expect+双端 actual、JvmDatabase、JNA 5.17.0（jvmMain）；jvmTest 15 项零失败；`:app:assembleMusesDebug` 通过。
- [x] S2 JvmPlayerPort：VLCJ 解码 + 本地队列状态机（repeat/shuffle 对齐 `QueueSnapshotData`）+ 状态/错误桥接（STATE_* 整型映射 + 8 条安全文案）+ 持久化/恢复复用；WebDAV 用 Ktor Range + okio spiller（500MB LRU 语义）。
  - 已交付：纯 JVM `:desktop` 模块（VLCJ 只进本模块，未建 composeApp-CMP，S3 建 UI 壳时再定）；`:desktop:test` 14 项零失败；`jvmTest` 无回归；`assembleMusesDebug` 通过。
- [x] S3a composeApp 桌面壳 + 自绘标题栏（CMP 1.12.0-rc01 兼容 Kotlin 2.4.10；:composeApp:run 可启动窗口）。
- [x] S3b 三屏最小可用：composeApp 内重写库房/播放/设置三屏（侧边导航双栏 + 曲目列表 + 播放控制 + WebDAV 音源增删，DPAPI 凭据）。
- [ ] S4 打包分发：`composeApp(desktop)` + `packageMsi`/`packageExe`（VLCJ 原生库随包）+ 签名；产出首版安装包；干净机安装验证。
- [ ] S5 回归：安卓包 `:app:assembleMusesDebug` 全程通过；feature 屏 CMP 化前后安卓行为对照。

## 验证命令

```bash
# S1 数据层（jvm）
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:compileKotlinJvm :core:common:jvmTest
# S5 安卓零影响门禁（P3 全程）
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug testDebugUnitTest
# S4 打包（模块名以实际 composeApp 命名为准）
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :composeApp:packageMsi :composeApp:packageExe
```

## 风险文件

- `PlayerPort.kt`（接口冻结，只增二期 TODO，不改现有签名）。
- `TabsLayout.kt` / `PlayerScreen.kt` / `LibraryGridPages.kt`（CMP 化剥离安卓相关，diff 逐行注明安卓行为不变）。
- `DataStorePath.jvm.kt`（TODO 转真实路径，Win 路径策略原型期定）。
- `PlaybackModels.kt`（队列语义对齐，不增桌面专属字段进 commonMain）。

## 回滚点

- composeApp 为新模块：删除即回滚；VLCJ 零渗入 commonMain。
- 每阶段单提交；`git revert` 即可。超预估 50% 即回父任务重估，禁止硬扛（迁移纲领通用门禁）。
