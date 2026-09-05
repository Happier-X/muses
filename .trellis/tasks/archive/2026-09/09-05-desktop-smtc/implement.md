# 执行计划：桌面端 SMTC 系统媒体控制

> 前置：`research/smtc-interop.md` 调研完成，design.md 中 [待核对] 常量已核对。

## 步骤

### 1. 核对调研结论 [gate]
- [ ] 阅读 `research/smtc-interop.md`，确认：interop IID、各接口方法序、Button 枚举值、JNA COM 回调案例、封面降级结论
- [ ] 有冲突则以调研报告（官方文档来源）为准，回改 design.md

### 2. 实现基础层（:desktop smtc 包）
- [ ] `WinRtRuntime.kt`：`Native.load("combase")` 声明 4 个函数 + HSTRING 助手
- [ ] `SmtcInterop.kt`：IID 常量 + vtable 调用助手 + ButtonPressed COM 回调对象
- [ ] `SmtcSession.kt` 接口 + `SmtcWinRtSession.kt` 真实实现（GetForWindow/启用按钮/状态与元数据更新）
- 验证：`./gradlew :desktop:compileKotlin`

### 3. 实现 SmtcController 门面
- [ ] hwnd 查找（EnumWindows 过滤当前进程 + 标题，轮询重试）
- [ ] 单线程 daemon executor + RoInitialize(MTA)
- [ ] StateFlow 订阅：metadata combine、timeline sample(1s)、PlaybackStatus 映射
- [ ] 全链路 try-catch 静默降级 + errorLog
- 验证：`./gradlew :desktop:test`

### 4. 单元测试
- [ ] `SmtcControllerTest`（fake SmtcSession）：状态映射（Playing/Paused/Stopped）、update 异常不扩散、uninstall 幂等、非 Windows 环境安全
- 验证：`./gradlew :desktop:test`

### 5. 接线（composeApp jvmMain）
- [ ] `Main.kt`：combine 元数据流 + `smtc.install/uninstall`（与 tray 并列 DisposableEffect）
- [ ] `JvmPlayerPort.updateSystemMediaTransport` 注释指向 SmtcController
- 验证：`./gradlew :composeApp:compileKotlinDesktop`

### 6. 全量检查 [last-iteration 全范围]
- [ ] `./gradlew :desktop:build :composeApp:compileKotlinDesktop`
- [ ] trellis-check 子代理全量质量检查

### 7. 提交与收尾
- [ ] 提交（仅本任务文件：`desktop/src/*/.../smtc/`、`composeApp/src/jvmMain/.../Main.kt`、`JvmPlayerPort.kt` 注释、任务目录）
- [ ] 3.3 spec 更新（若有桌面层沉淀）
- [ ] 归档任务；更新记忆 desktop-parallel-work-status.md

## 回滚点

- 步骤 2-4 失败：`git checkout -- desktop/`，无外部影响
- 步骤 5 后异常：移除 Main.kt 接线即可回退运行时行为

## 验证命令速查

```bash
./gradlew :desktop:compileKotlin
./gradlew :desktop:test
./gradlew :composeApp:compileKotlinJvm
```

## 人工验收（提交后由用户执行）

- AC1–AC4：播放歌曲 → 任务栏媒体浮层显示与按键（同托盘任务先例，自动化无法覆盖）
