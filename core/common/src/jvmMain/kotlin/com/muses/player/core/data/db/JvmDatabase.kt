package com.muses.player.core.data.db

import androidx.room.Room
import com.muses.player.core.data.platform.PlatformDirs
import java.io.File

/**
 * S1 桌面 Room 接线（design §5）。
 *
 * - DB 文件：`<appDataDir>/muses.db`，DB 名冻结，与安卓侧 DatabaseModule.DB_NAME 一致；
 * - 升级链：MIGRATION_1_2/2_3/3_4/4_5/5_6 全挂载，schema v6 冻结；
 * - 驱动/查询上下文：统一收口 commonMain [getRoomDatabase]
 *   （BundledSQLiteDriver + IO 上下文），桌面侧不另起实现；
 * - JVM Room.databaseBuilder 口径：`Room.databaseBuilder(path, ctor)`，
 *   构造器取 KSP 生成的 jvmMain MusesDatabaseConstructor（[MusesDatabaseConstructor]）。
 *
 * S2 交接：composeApp(desktop) 的 Koin 模块调 [createJvmDatabase] 拿单例即可；
 * 内存库走 [createJvmInMemoryDatabase]（测试/原型用）。
 */
const val JVM_DB_NAME = "muses.db"

fun createJvmDatabase(): MusesDatabase = getRoomDatabase(
    Room.databaseBuilder<MusesDatabase>(
        File(PlatformDirs.appDataDir(), JVM_DB_NAME).absolutePath,
        MusesDatabaseConstructor::initialize,
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6),
)

/** 内存库（测试/原型用，不落盘）。 */
fun createJvmInMemoryDatabase(): MusesDatabase = getRoomDatabase(
    Room.inMemoryDatabaseBuilder<MusesDatabase>(
        MusesDatabaseConstructor::initialize,
    ),
)

/** 供测试指定路径建库（隔离 tmp 目录，避免污染真实 %APPDATA%）。 */
fun createJvmDatabaseAt(path: String): MusesDatabase = getRoomDatabase(
    Room.databaseBuilder<MusesDatabase>(
        path,
        MusesDatabaseConstructor::initialize,
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6),
)
