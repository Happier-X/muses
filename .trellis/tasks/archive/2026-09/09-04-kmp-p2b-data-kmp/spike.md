# P2b S0 Spike 结论（R1 门禁）

> 事后补记：spike 验证文件已删，本结论依据实施过程记录 + S1–S4 全量验证回溯确认。

## 验证项与结论

| # | 验证项 | 结论 |
|---|---|---|
| 1 | room-gradle-plugin × `com.android.kotlin.multiplatform.library`（AGP 9） | ✅ 无冲突，`:core:common` 正常配置 |
| 2 | `kspCommonMainMetadata` + `kspAndroid` + `kspJvm` 三路（KSP 2.3.11 × Kotlin 2.4.10） | ✅ 照常工作，无需升级 |
| 3 | 空 entity/DAO 在 commonMain 编译 + jvm 侧 `BundledSQLiteDriver` 读写 | ✅ 通过 |
| 4 | `Migration` 空类在 commonMain 编译 | ✅ 通过（后发现 `SupportSQLiteDatabase` 不可用，S1 改写为 `SQLiteConnection.prepare/step`，SQL 逐字保留） |

## 决策

spike 通过，进入 R2。遗留约束记入 design §5：`sqlite-bundled` 取 2.6.2（room 2.8.4 约束对齐）；`@ConstructedBy` 为 Room-KMP 必需。
