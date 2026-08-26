package com.muses.player.core.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 迁移回归测试（任务 08-25-native-playback-persistence / P2）。
 *
 * 说明：Room 2.8 的 MigrationTestHelper 在 Robolectric 下存在 KMP driver 路径冲突，
 * 故改为「手建旧版 schema → 直接执行迁移对象 → PRAGMA 校验列」的方式，
 * 迁移 SQL 本身与生产代码同一来源（MIGRATION_* 对象）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** 以指定版本号创建内存库，并用 provided 建表回调搭建旧版 schema */
    private fun openWritableDatabase(version: Int, onCreate: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // 内存库
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return factory.create(configuration).writableDatabase
    }

    @Test
    fun `MIGRATION_4_5_sources新增username列`() {
        val db = openWritableDatabase(4) { sql ->
            sql.execSQL(
                "CREATE TABLE IF NOT EXISTS `sources` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, `url` TEXT, `path` TEXT, `createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            sql.execSQL(
                "INSERT INTO `sources` VALUES ('src1','NAS','WEBDAV','https://dav.example.com',NULL,1000,2000)",
            )
        }

        MIGRATION_4_5.migrate(db)

        db.query("SELECT `username` FROM `sources` WHERE `id` = 'src1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(null, cursor.getString(0)) // 迁移后默认 NULL
        }
    }

    @Test
    fun `MIGRATION_3_4_songs新增刮削六列`() {
        val db = openWritableDatabase(3) { sql ->
            sql.execSQL(
                "CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, " +
                    "`sourceType` TEXT NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`artist` TEXT, `albumTitle` TEXT, `durationMs` INTEGER NOT NULL, " +
                    "`durationSec` INTEGER NOT NULL, `coverUri` TEXT, `lyrics` TEXT, " +
                    "`replayGainTrackDb` REAL, `tagsVersion` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }

        MIGRATION_3_4.migrate(db)

        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info(`songs`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) columns.add(cursor.getString(nameIdx))
        }
        for (col in listOf("lyricsFormat", "lyricsSource", "metaTitle", "metaArtist", "metaAlbum", "metaCover")) {
            assertTrue("缺列 $col", columns.contains(col))
        }
    }
}
