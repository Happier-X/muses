# P2a 技术设计

## 1. 依赖变更（version catalog）

```toml
[versions] koin-bom = "4.2.0"
[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin-bom" }
koin-core / koin-android / koin-compose /
koin-compose-viewmodel / koin-androidx-compose-navigation   # 无 version（BOM 统一）
```

- 删：`hilt-*` libraries + `hilt` 插件条目；各模块删 `ksp(hilt.compiler)` / `ksp(androidx.hilt.compiler)`（ksp 插件本身保留，Room 还在用）。
- 各模块按需 `implementation(platform(koin-bom)) + 对应工件`；禁止散装版本号（AC3）。

## 2. 映射表（Hilt → Koin）

| Hilt 现状 | Koin 写法 | 备注 |
|---|---|---|
| `@HiltAndroidApp MusesApplication` + 字段 `@Inject` | `onCreate { startKoin { androidContext(this@App); modules(appModules) } }` + `by inject()` | 字段注入改懒委托 |
| `@Module @InstallIn(SingletonComponent)` + `@Provides @Singleton` | `module { single { ... } }` | 7 个 Module 逐个转，`@Provides` 非单例看原作用域→`factory` |
| `@Binds abstract fun bindX(impl: Impl): Iface` | `singleOf(::Impl) { bind<Iface>() }` | 双绑定：`{ bind<A>(); bind<B>() }`（ErrorLogStore case） |
| `@HiltViewModel class VM @Inject ctor` | 去注解 + `viewModel { VM(get(), ...) }` 声明 | 声明集中放各模块 `*KoinModule.kt`（与原 Module 同目录） |
| `hiltViewModel()`（25 处） | `koinViewModel()`（`org.koin.androidx.compose`） | `MusesApp.kt` 全限定名调用一并改 |
| `HiltWorkerFactory` + `EntryPointAccessors`（ScanWorker） | Worker 实现 `KoinComponent`，`by inject()` 懒取；删 factory 接线 | D3：行为不变，不引入 workmanager 插件 |

## 3. 模块声明归属

- 每个原 `@Module` 文件旁新建同名 Koin module（如 `DatabaseModule.kt` → 同文件改写为 `val databaseModule = module { ... }`，删文件比建文件更少 diff——直接原文件改写）。
- 聚合：`app` 建 `appModule` 聚合各 core/feature modules（`includes(...)` 或 `modules(...)` 列表），`startKoin` 一处。

## 4. 验证矩阵

| 门禁 | 命令 | 期望 |
|---|---|---|
| 零残留 | AC1 grep | 零命中 |
| 编译+测试 | `:app:assembleMusesDebug :app:lintMusesDebug testDebugUnitTest` | 通过（spec 标准门禁） |
| 运行冒烟 | 安装 musesDebug，启动→扫库→播放→刮削一遍 | 无崩溃、无注入失败（Koin `NoBeanDefFoundException` 即红） |
| Koin 校验 | `startKoin` 处可选 `checkModules()`（debug） | 无定义缺失 |

## 5. 回滚

- 单提交原则，失败 `git revert`。Koin 切换是原子性的，不可半切（双轨禁令 AC4）。
