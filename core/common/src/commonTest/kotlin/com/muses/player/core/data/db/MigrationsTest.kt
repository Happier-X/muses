package com.muses.player.core.data.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 迁移回归测试（P2b-S3 由 :core:data Robolectric 版改写搬入）。
 *
 * 搬入原因：BundledSQLiteDriver 的 Android native 库在 Robolectric（JVM）下
 * UnsatisfiedLinkError；commonTest 的 jvm 变体用桌面 native，可跑。
 * 迁移 SQL 与生产代码同一来源（MIGRATION_* 对象），断言语义与改写前一致。
 * DB 名 muses.db、schema v6 冻结，升级链不断。
 */
class MigrationsTest {

    /** 内存库 connection + 建表回调搭建旧版 schema，执行 block 后关闭 */
    private fun withMemoryDatabase(version: Int, onCreate: (SQLiteConnection) -> Unit, block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.exec("PRAGMA user_version = $version")
            onCreate(connection)
            block(connection)
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.exec(sql: String) {
        prepare(sql).use { it.step() }
    }

    private fun SQLiteConnection.tableColumns(table: String): List<String> {
        val columns = mutableListOf<String>()
        prepare("PRAGMA table_info(`$table`)").use { stmt ->
            while (stmt.step()) columns.add(stmt.getText(1))
        }
        return columns
    }

    private fun SQLiteConnection.querySingleText(sql: String): String? {
        prepare(sql).use { stmt ->
            if (!stmt.step()) return null
            // getText 遇 NULL 返回空串，先判 isNull 还原旧 cursor.getString 语义
            return if (stmt.isNull(0)) null else stmt.getText(0)
        }
    }

    @Test
    fun migration_4_5_sources新增username列() {
        withMemoryDatabase(4, onCreate = { connection ->
            connection.exec(
                "CREATE TABLE IF NOT EXISTS `sources` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, `url` TEXT, `path` TEXT, `createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            connection.exec(
                "INSERT INTO `sources` VALUES ('src1','NAS','WEBDAV','https://dav.example.com',NULL,1000,2000)",
            )
        }) { connection ->
            MIGRATION_4_5.migrate(connection)

            // 迁移后默认 NULL
            assertEquals(null, connection.querySingleText("SELECT `username` FROM `sources` WHERE `id` = 'src1'"))
        }
    }

    @Test
    fun migration_5_6_移除replayGainTrackDb列() {
        withMemoryDatabase(5, onCreate = { connection ->
            connection.exec(
                "CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, " +
                    "`sourceType` TEXT NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`artist` TEXT, `albumTitle` TEXT, `durationMs` INTEGER NOT NULL, " +
                    "`durationSec` INTEGER NOT NULL, `coverUri` TEXT, `lyrics` TEXT, `lyricsFormat` TEXT, " +
                    "`lyricsSource` TEXT, `metaTitle` TEXT, `metaArtist` TEXT, `metaAlbum` TEXT, `metaCover` TEXT, " +
                    "`replayGainTrackDb` REAL, `tagsVersion` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            connection.exec("CREATE INDEX IF NOT EXISTS `index_songs_sourceId` ON `songs` (`sourceId`)")
            connection.exec(
                "INSERT INTO `songs` VALUES ('s1','src','LOCAL','/a.mp3','t','ar','al',1000,1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL, -6.5, 1)",
            )
        }) { connection ->
            MIGRATION_5_6.migrate(connection)

            val columns = connection.tableColumns("songs")
            assertFalse(columns.contains("replayGainTrackDb"), "不应再有 replayGainTrackDb")
            assertTrue(columns.contains("title"))
            assertEquals("s1", connection.querySingleText("SELECT `id` FROM `songs` WHERE `id`='s1'"))
        }
    }

    @Test
    fun migration_3_4_songs新增刮削六列() {
        withMemoryDatabase(3, onCreate = { connection ->
            connection.exec(
                "CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, " +
                    "`sourceType` TEXT NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`artist` TEXT, `albumTitle` TEXT, `durationMs` INTEGER NOT NULL, " +
                    "`durationSec` INTEGER NOT NULL, `coverUri` TEXT, `lyrics` TEXT, " +
                    "`replayGainTrackDb` REAL, `tagsVersion` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }) { connection ->
            MIGRATION_3_4.migrate(connection)

            val columns = connection.tableColumns("songs")
            for (col in listOf("lyricsFormat", "lyricsSource", "metaTitle", "metaArtist", "metaAlbum", "metaCover")) {
                assertTrue(columns.contains(col), "缺列 $col")
            }
        }
    }
}
