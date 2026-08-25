package com.muses.player.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.muses.player.core.data.db.MIGRATION_1_2
import com.muses.player.core.data.db.MIGRATION_2_3
import com.muses.player.core.data.db.MIGRATION_3_4
import com.muses.player.core.data.db.MIGRATION_4_5
import com.muses.player.core.data.db.MusesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    private const val DB_NAME = "muses.db"
    private const val DATASTORE_NAME = "muses_settings.preferences_pb"

    @Provides
    @Singleton
    fun provideMusesDatabase(@ApplicationContext context: Context): MusesDatabase =
        Room.databaseBuilder(context, MusesDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideSongDao(db: MusesDatabase): com.muses.player.core.data.dao.SongDao = db.songDao()

    @Provides
    fun provideSourceDao(db: MusesDatabase): com.muses.player.core.data.dao.SourceDao = db.sourceDao()

    @Provides
    fun provideAlbumDao(db: MusesDatabase): com.muses.player.core.data.dao.AlbumDao = db.albumDao()

    @Provides
    fun provideArtistDao(db: MusesDatabase): com.muses.player.core.data.dao.ArtistDao = db.artistDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            File(context.filesDir, DATASTORE_NAME)
        }
}
