package com.muses.player.core.data.repository

import com.muses.player.core.data.dao.AlbumDao
import com.muses.player.core.data.dao.ArtistDao
import com.muses.player.core.data.dao.SourceDao
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.mapper.toEntity
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 曲库仓库 */
interface SongRepository {
    fun observeSongs(): Flow<List<Song>>
    /** 按 sourceId 替换该音源下全部歌曲（扫描完成后调用） */
    suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>)
    /** 按 id 取单曲（M3 刮削写回链路） */
    suspend fun getSong(id: String): Song?
    /** 单曲写入/更新（M3 刮削写回链路，对齐 Web upsertSong） */
    suspend fun upsert(song: Song)
}

@Singleton
class RoomSongRepository @Inject constructor(
    private val songDao: SongDao,
) : SongRepository {
    override fun observeSongs(): Flow<List<Song>> =
        songDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>) {
        songDao.replaceSourceSongs(sourceId, songs.map { it.toEntity() })
    }

    override suspend fun getSong(id: String): Song? = songDao.getById(id)?.toDomain()

    override suspend fun upsert(song: Song) {
        songDao.upsert(song.toEntity())
    }
}

/** 音源仓库 */
interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun getSource(id: String): Source?
    suspend fun upsert(source: Source)
    suspend fun deleteById(id: String)
}

@Singleton
class RoomSourceRepository @Inject constructor(
    private val sourceDao: SourceDao,
) : SourceRepository {
    override fun observeSources(): Flow<List<Source>> =
        sourceDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSource(id: String): Source? =
        sourceDao.getById(id)?.toDomain()

    override suspend fun upsert(source: Source) {
        sourceDao.upsert(source.toEntity())
    }

    override suspend fun deleteById(id: String) {
        sourceDao.deleteById(id)
    }
}

/** 专辑仓库 */
interface AlbumRepository {
    fun observeAlbums(): Flow<List<Album>>
    fun observeAlbumWithSongs(albumId: String): Flow<com.muses.player.core.data.db.AlbumWithSongs?>
}

@Singleton
class RoomAlbumRepository @Inject constructor(
    private val albumDao: AlbumDao,
) : AlbumRepository {
    override fun observeAlbums(): Flow<List<Album>> =
        albumDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeAlbumWithSongs(albumId: String): Flow<com.muses.player.core.data.db.AlbumWithSongs?> =
        albumDao.observeAlbumWithSongs(albumId)
}

/** 艺术家仓库 */
interface ArtistRepository {
    fun observeArtists(): Flow<List<Artist>>
    fun observeArtistWithSongs(artistId: String): Flow<com.muses.player.core.data.db.ArtistWithSongs?>
}

@Singleton
class RoomArtistRepository @Inject constructor(
    private val artistDao: ArtistDao,
) : ArtistRepository {
    override fun observeArtists(): Flow<List<Artist>> =
        artistDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeArtistWithSongs(artistId: String): Flow<com.muses.player.core.data.db.ArtistWithSongs?> =
        artistDao.observeArtistWithSongs(artistId)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun bindSongRepository(impl: RoomSongRepository): SongRepository

    @Binds
    abstract fun bindSourceRepository(impl: RoomSourceRepository): SourceRepository

    @Binds
    abstract fun bindAlbumRepository(impl: RoomAlbumRepository): AlbumRepository

    @Binds
    abstract fun bindArtistRepository(impl: RoomArtistRepository): ArtistRepository

    @Binds
    abstract fun bindPlaylistRepository(impl: RoomPlaylistRepository): PlaylistRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindCredentialsRepository(impl: AndroidKeyStoreCredentialsRepository): CredentialsRepository

    @Binds
    @Singleton
    abstract fun bindCryptoEngine(impl: AndroidKeystoreCryptoEngine): CryptoEngine
}
