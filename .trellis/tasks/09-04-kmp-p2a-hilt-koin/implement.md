# P2a 执行计划

## 有序清单

- [x] S0 空壳探针：Koin 4.2 在 Kotlin 2.4.10 下可用（包路径按 4.x 新路径）。
- [x] S1 core 层 7 Module 转 DSL（含双绑定显式委托）；`@Inject` 去注解，业务零改动。
- [x] S2 21 个 ViewModel + 25 处调用点切换（含 entry 共享作用域对等改写）。
- [x] S3 入口去注解；ScanWorker 转 KoinComponent；factory 接线删除。
- [x] S4 Hilt 全家删除，AC1 零命中。
- [x] S5 全量回归通过（主会话实跑 BUILD SUCCESSFUL）；真机冒烟待用户执行。

## 验证命令（一键）

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest
grep -rn "dagger.hilt\|androidx.hilt\|hiltViewModel\|HiltWorkerFactory\|EntryPointAccessors" --include="*.kt" --include="*.kts" --include="*.toml" app core feature gradle
```

## 风险文件

- `MusesApplication.kt`（启动入口，改坏即闪退，S0 先行验证）。
- `Repositories.kt`（双绑定，最易写错，新人区）。
- `MusesApp.kt`（25 处调用大户，漏改即编译错，grep 兜底）。
