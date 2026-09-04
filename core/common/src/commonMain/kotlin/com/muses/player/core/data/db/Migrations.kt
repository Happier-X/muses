package com.muses.player.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * 曲库升级链（v1→v6）。P2b 由 SupportSQLiteDatabase 口径改写为 KMP 口径
 * （migrate(SQLiteConnection) + prepare/step），SQL 语句逐字保留、语义冻结。
 * DB 名 muses.db、schema v6、key 名冻结，禁止任何 schema 改动（design §5 回滚红线）。
 */
private fun SQLiteConnection.exec(sql: String) {
    prepare(sql).use { it.step() }
}

/** v1 → v2：新增播放列表两表（向前追加，不改既有表） */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.exec(
            "CREATE TABLE IF NOT EXISTS `playlists` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        connection.exec(
            "CREATE TABLE IF NOT EXISTS `playlist_songs` (" +
                "`playlistId` TEXT NOT NULL, " +
                "`songId` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "PRIMARY KEY(`playlistId`, `position`), " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        connection.exec(
            "CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)",
        )
    }
}

/** v2 → v3：songs 表新增 ReplayGain 列（向前追加，不改既有表） */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.exec("ALTER TABLE `songs` ADD COLUMN `replayGainTrackDb` REAL DEFAULT NULL")
    }
}

/** v3 → v4：songs 表新增刮削写回列：歌词格式/来源 + 字段来源平铺（向前追加，不改既有表） */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.exec("ALTER TABLE `songs` ADD COLUMN `lyricsFormat` TEXT DEFAULT NULL")
        connection.exec("ALTER TABLE `songs` ADD COLUMN `lyricsSource` TEXT DEFAULT NULL")
        connection.exec("ALTER TABLE `songs` ADD COLUMN `metaTitle` TEXT DEFAULT NULL")
        connection.exec("ALTER TABLE `songs` ADD COLUMN `metaArtist` TEXT DEFAULT NULL")
        connection.exec("ALTER TABLE `songs` ADD COLUMN `metaAlbum` TEXT DEFAULT NULL")
        connection.exec("ALTER TABLE `songs` ADD COLUMN `metaCover` TEXT DEFAULT NULL")
    }
}

/** v4 → v5：sources 表新增 WebDAV 登录名 username（歌词任务 L4，向前追加，不改既有表） */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.exec("ALTER TABLE `sources` ADD COLUMN `username` TEXT DEFAULT NULL")
    }
}

/** v5 → v6：移除 songs.replayGainTrackDb（移除音量均衡，重建表兼容旧 SQLite） */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.exec(
            "CREATE TABLE IF NOT EXISTS `songs_new` (" +
                "`id` TEXT NOT NULL, " +
                "`sourceId` TEXT NOT NULL, " +
                "`sourceType` TEXT NOT NULL, " +
                "`path` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`artist` TEXT, " +
                "`albumTitle` TEXT, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`durationSec` INTEGER NOT NULL, " +
                "`coverUri` TEXT, " +
                "`lyrics` TEXT, " +
                "`lyricsFormat` TEXT, " +
                "`lyricsSource` TEXT, " +
                "`metaTitle` TEXT, " +
                "`metaArtist` TEXT, " +
                "`metaAlbum` TEXT, " +
                "`metaCover` TEXT, " +
                "`tagsVersion` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        connection.exec(
            "INSERT INTO `songs_new` " +
                "(`id`,`sourceId`,`sourceType`,`path`,`title`,`artist`,`albumTitle`,`durationMs`,`durationSec`,`coverUri`,`lyrics`,`lyricsFormat`,`lyricsSource`,`metaTitle`,`metaArtist`,`metaAlbum`,`metaCover`,`tagsVersion`) " +
                "SELECT `id`,`sourceId`,`sourceType`,`path`,`title`,`artist`,`albumTitle`,`durationMs`,`durationSec`,`coverUri`,`lyrics`,`lyricsFormat`,`lyricsSource`,`metaTitle`,`metaArtist`,`metaAlbum`,`metaCover`,`tagsVersion` FROM `songs`",
        )
        connection.exec("DROP TABLE `songs`")
        connection.exec("ALTER TABLE `songs_new` RENAME TO `songs`")
        connection.exec("CREATE INDEX IF NOT EXISTS `index_songs_sourceId` ON `songs` (`sourceId`)")
        connection.exec("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)")
        connection.exec("CREATE INDEX IF NOT EXISTS `index_songs_albumTitle` ON `songs` (`albumTitle`)")
    }
}
