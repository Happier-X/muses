package com.muses.player.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import java.io.File
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 数据库装配（P2a Hilt→Koin：原 `@Module @InstallIn(SingletonComponent)`）。
 * `@Provides @Singleton`→`single`；无作用域的 DAO 提供→`factory`（见 design.md 映射表）。
 */
val databaseModule = module {

    single<MusesDatabase> {
        Room.databaseBuilder(androidContext(), MusesDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    factory<SongDao> { get<MusesDatabase>().songDao() }

    factory<SourceDao> { get<MusesDatabase>().sourceDao() }

    factory<AlbumDao> { get<MusesDatabase>().albumDao() }

    factory<ArtistDao> { get<MusesDatabase>().artistDao() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            File(androidContext().filesDir, DATASTORE_NAME)
        }
    }
}

private const val DB_NAME = "muses.db"
private const val DATASTORE_NAME = "muses_settings.preferences_pb"
