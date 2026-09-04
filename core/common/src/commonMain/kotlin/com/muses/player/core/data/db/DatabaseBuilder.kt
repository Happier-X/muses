package com.muses.player.core.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * P2b-S3 平台无关的 Room 装配收口（design §3）。
 * 平台侧只负责 `Room.databaseBuilder` 拿到 Builder，这里统一挂
 * BundledSQLiteDriver + IO 查询上下文后 build。
 * DB 名 / key 名 / schema 版本冻结，行为与旧 `Room.databaseBuilder(...).build()` 一致。
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<MusesDatabase>): MusesDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
