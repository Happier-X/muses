package com.muses.player.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.muses.player.core.data.dao.AlbumDao
import com.muses.player.core.data.dao.ArtistDao
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.dao.SourceDao
import com.muses.player.core.data.db.MIGRATION_1_2
import com.muses.player.core.data.db.MIGRATION_2_3
import com.muses.player.core.data.db.MIGRATION_3_4
import com.muses.player.core.data.db.MIGRATION_4_5
import com.muses.player.core.data.db.MIGRATION_5_6
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.getRoomDatabase
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.data.store.initDataStoreDir
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 数据库装配（P2a Hilt→Koin：原 `@Module @InstallIn(SingletonComponent)`）。
 * `@Provides @Singleton`→`single`；无作用域的 DAO 提供→`factory`（见 design.md 映射表）。
 *
 * P2b-S3：平台 builder 接线——`Room.databaseBuilder` 拿 Builder（DB 名 muses.db 冻结，
 * 5 个 Migration 原样挂载），收口到 commonMain [getRoomDatabase]
 *（BundledSQLiteDriver + IO 上下文）；DataStore 改 KMP 口径 createWithPath
 *（文件名/路径与旧 `File(filesDir, DATASTORE_NAME)` 同文件，行为零变化）。
 */
val databaseModule = module {

    single<MusesDatabase> {
        getRoomDatabase(
            Room.databaseBuilder<MusesDatabase>(androidContext(), DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6),
        )
    }

    factory<SongDao> { get<MusesDatabase>().songDao() }

    factory<SourceDao> { get<MusesDatabase>().sourceDao() }

    factory<AlbumDao> { get<MusesDatabase>().albumDao() }

    factory<ArtistDao> { get<MusesDatabase>().artistDao() }

    single<DataStore<Preferences>> {
        initDataStoreDir(androidContext().filesDir)
        createDataStore()
    }
}

private const val DB_NAME = "muses.db"
