package com.muses.player.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 → v2：新增播放列表两表（向前追加，不改既有表） */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
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
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_songs_songId` ON `playlist_songs` (`songId`)",
        )
    }
}

/** v2 → v3：songs 表新增 ReplayGain 列（向前追加，不改既有表） */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `replayGainTrackDb` REAL DEFAULT NULL")
    }
}

/** v3 → v4：songs 表新增刮削写回列：歌词格式/来源 + 字段来源平铺（向前追加，不改既有表） */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `lyricsFormat` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `lyricsSource` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `metaTitle` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `metaArtist` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `metaAlbum` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `songs` ADD COLUMN `metaCover` TEXT DEFAULT NULL")
    }
}
