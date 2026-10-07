package com.muses.player.core.data.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioQualityMigrationTest {
    @Test fun `音质迁移只追加字段并保留现有歌曲`() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.prepare("CREATE TABLE songs (id TEXT PRIMARY KEY, title TEXT NOT NULL)").use { it.step() }
            connection.prepare("INSERT INTO songs VALUES ('existing', '保留的歌曲')").use { it.step() }
            MIGRATION_8_9.migrate(connection)
            connection.prepare("SELECT id, title, audioQuality FROM songs").use {
                assertTrue(it.step())
                assertEquals("existing", it.getText(0))
                assertEquals("保留的歌曲", it.getText(1))
                assertTrue(it.isNull(2))
            }
        } finally { connection.close() }
    }
}
