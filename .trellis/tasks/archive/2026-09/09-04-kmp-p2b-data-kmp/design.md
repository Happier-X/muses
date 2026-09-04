# P2b 技术设计

## 1. 落点布局（`:core:common`）

```
commonMain/.../core/
  data/{db/{Entities,PlaylistEntities,MusesDatabase,Migrations}.kt,
        dao/{Song,Source,Album,Artist,Playlist}Dao.kt,
        repository/{Settings,PlaybackState,RecentPlays}Repository.kt,   # 纯 DataStore 逻辑（复核）
        store/DataStoreFactory.kt}      # createWithPath + expect 路径
  scrape/{queue/{ScrapeQueueStore,ScrapeHistoryStore},
          writeback/RollbackJournalStore}.kt                            # 纯 DataStore 逻辑（复核）
androidMain/.../core/data/store/
  DataStorePath.android.kt              # actual: context.filesDir
  DatabaseBuilder.android.kt            # actual: Room.databaseBuilder(...)
jvmMain/...                             # 占位 actual（P3 实现真实路径）
```

- 留守：`CredentialsRepository`（Keystore）、`CrashHandler`（files）、`AudioTagReader`（java.io）、`SongFileWriters`/`WritebackOrchestrator`（core:data 运行时）、全部 provider。
- 包名不变，消费者 import 零改动；Koin `databaseModule` 只换构造方式。

## 2. 构建配置

- catalog 加：`sqlite-bundled`（与 room-runtime 同版本线，优先稳定版，无则取官方文档同期 alpha 并记录）、`androidx-room-gradle-plugin`（`androidx.room`，与 room 同版本）。
- `:core:common` 加 `alias(libs.plugins.androidx.room)` + `commonMain { room-runtime, sqlite-bundled, datastore-preferences }`；KSP：`kspCommonMainMetadata(room-compiler)` + `kspAndroid(room-compiler)` + `kspJvm(room-compiler)`。
- schemas 导出目录指到 `core/common/schemas`（`room.schemaDirectory`），旧 `core/data/schemas` 迁移后删除。

## 3. 安卓接线（行为冻结）

```kotlin
// commonMain
expect fun dataStorePath(fileName: String): String
fun createDataStore() = PreferenceDataStoreFactory.createWithPath { dataStorePath(DATASTORE_NAME).toPath() }
fun getRoomDatabase(builder: RoomDatabase.Builder<MusesDatabase>) = builder
    .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
// androidMain actual: filesDir；builder actual: Room.databaseBuilder(ctx, MusesDatabase::class.java, "muses.db")
```

- DB 名 `muses.db`、DataStore 文件名、`playback_snapshot` 等 key、schema v6、5 个 Migration：全部冻结，原样搬。
- `MigrationTest` 留守 `:core:data`（或 app androidTest），改写为测平台 builder 产物。

## 4. 验证矩阵

| 门禁 | 命令/动作 | 期望 |
|---|---|---|
| spike | 空表+jvm 读写 | 通过才进 R2 |
| 纯度 | `compileKotlinJvm` + commonMain import 审计 | 零 `java.*/android.*` |
| 回归 | `assembleMusesDebug + testDebugUnitTest` | 通过 |
| 实测 | MuMu：冷启动→扫库→杀进程→重启恢复队列→设置改值→重启保持 | 数据不断、升级不丢 |

## 5. 回滚

- 单提交；失败 `git revert`。DB 相关回滚红线：若已发布则 Migration 不可逆——P2b 为开发中版本，v6 内无 schema 变更，故 revert 安全（design 约束：R2–R4 禁止任何 schema 改动）。
