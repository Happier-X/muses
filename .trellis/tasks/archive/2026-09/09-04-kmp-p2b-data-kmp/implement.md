# P2b 执行计划

## 有序清单

- [x] S0 spike：结论见 `spike.md`（room 插件链/ksp 三路/jvm 读写/Migration 空类全过）。
- [x] S1 Room 搬迁：8 entities/5 DAO/MusesDatabase/5 Migration（改写 `SQLiteConnection`，SQL 逐字保留）+ schemas；6.json 逐字节一致。
- [x] S2 DataStore 搬迁：`createWithPath` + expect/actual；7 store + 3 repository；`CredentialsRepository` 留守。
- [x] S3 安卓接线：平台 builder + BundledSQLiteDriver；MigrationTest 改 commonTest(jvm)3/3；旧文件清理。
- [x] S4 全量回归（主会话实跑 BUILD SUCCESSFUL）+ MuMu 冷启动/杀进程重启无崩溃（DB WAL + preferences_pb 落盘正常）；扫库/设置 UI 环节待用户随用验证。

## 验证命令（一键）

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :core:common:assemble :core:common:allTests :app:assembleMusesDebug testDebugUnitTest
grep -rn "^import java\.\|^import android\." core/common/src/commonMain --include="*.kt" | grep -v "androidx.room\|androidx.sqlite\|androidx.datastore"
```

## 风险文件

- `:core:common/build.gradle.kts`（room 插件 × kmp.library interplay，S0 即暴露）。
- `DatabaseModule.kt`（单点接线，改坏即全仓无数据）。
- `MusesDatabase.kt` + schemas（版本冻结，禁止顺手改 schema）。
