package com.muses.player.core.data.db

import androidx.room.RoomDatabaseConstructor

/**
 * P2b-S3 Room KMP 构造器（@ConstructedBy 配套）。
 * KMP 下 Room 不可用反射构造 Database 实现，须由平台 actual 返回
 * 各平台 KSP 生成的 `MusesDatabase_Impl`。
 */
expect object MusesDatabaseConstructor : RoomDatabaseConstructor<MusesDatabase>
