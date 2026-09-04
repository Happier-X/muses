package com.muses.player.core.data.repository

import com.muses.player.core.data.dao.AlbumDao
import com.muses.player.core.data.dao.ArtistDao
import com.muses.player.core.data.dao.SourceDao
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.log.ErrorLogCrashPersistence
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.log.RingBufferErrorLogStore
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.mapper.toEntity
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/** 曲库仓库 */
interface SongRepository {
    fun observeSongs(): Flow<List<Song>>
    /** 按 sourceId 替换该音源下全部歌曲（扫描完成后调用） */
    suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>)
    /** 删除该音源下全部歌曲（删除音源时同步清理，对齐 Web reconcileSourceSongs(id, [])） */
    suspend fun deleteSourceSongs(sourceId: String)
    /** 从全库歌曲重建专辑/艺术家派生索引（存量库升级回填；写入路径已自动触发） */
    suspend fun rebuildDerivedIndexes()
    /** 按 id 取单曲（M3 刮削写回链路） */
    suspend fun getSong(id: String): Song?
    /** 单曲写入/更新（M3 刮削写回链路，对齐 Web upsertSong） */
    suspend fun upsert(song: Song)
}

class RoomSongRepository constructor(
    private val songDao: SongDao,
    private val albumDao: com.muses.player.core.data.dao.AlbumDao,
    private val artistDao: com.muses.player.core.data.dao.ArtistDao,
) : SongRepository {
    override fun observeSongs(): Flow<List<Song>> =
        songDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>) {
        songDao.replaceSourceSongs(sourceId, songs.map { it.toEntity() })
        rebuildDerivedIndexes()
    }

    override suspend fun deleteSourceSongs(sourceId: String) {
        songDao.deleteBySource(sourceId)
        rebuildDerivedIndexes()
    }

    override suspend fun getSong(id: String): Song? = songDao.getById(id)?.toDomain()

    override suspend fun upsert(song: Song) {
        songDao.upsert(song.toEntity())
        // 懒扫描回写可能改变专辑/艺术家归属，同步重建派生索引
        rebuildDerivedIndexes()
    }

    /**
     * 从全库 songs 重建 albums/artists 索引与 cross refs（派生数据，全量重建幂等）。
     * M1 遗留缺口补齐：albums/artists 表此前无任何写入方，专辑/艺术家页恒为空。
     * id 约定："album:<标题>" / "artist:<名称>"（稳定可读，详情路由内部自洽）；
     * 无标题归入「未知专辑」、无艺术家归入「未知艺术家」（与列表页兜底文案一致）。
     */
    override suspend fun rebuildDerivedIndexes() {
        val all = songDao.getAll()
        if (all.isEmpty()) {
            albumDao.deleteAll()
            artistDao.deleteAll()
            songDao.clearSongAlbumRefs()
            songDao.clearSongArtistRefs()
            return
        }

        val albumRefs = mutableListOf<com.muses.player.core.data.db.SongAlbumCrossRef>()
        val artistRefs = mutableListOf<com.muses.player.core.data.db.SongArtistCrossRef>()

        val albums = all
            .groupBy { it.albumTitle?.trim()?.takeIf { t -> t.isNotEmpty() } ?: "未知专辑" }
            .map { (title, group) ->
                val albumId = "album:$title"
                group.forEach { song ->
                    albumRefs += com.muses.player.core.data.db.SongAlbumCrossRef(song.id, albumId)
                }
                com.muses.player.core.data.db.AlbumEntity(
                    id = albumId,
                    title = title,
                    artist = group.mapNotNull { it.artist }.distinct().firstOrNull(),
                    songCount = group.size,
                )
            }

        val artists = all
            .groupBy { it.artist?.trim()?.takeIf { a -> a.isNotEmpty() } ?: "未知艺术家" }
            .map { (name, group) ->
                val artistId = "artist:$name"
                group.forEach { song ->
                    artistRefs += com.muses.player.core.data.db.SongArtistCrossRef(song.id, artistId)
                }
                com.muses.player.core.data.db.ArtistEntity(
                    id = artistId,
                    name = name,
                    songCount = group.size,
                )
            }

        albumDao.deleteAll()
        albumDao.insertAll(albums)
        artistDao.deleteAll()
        artistDao.insertAll(artists)
        songDao.clearSongAlbumRefs()
        songDao.insertSongAlbumRefs(albumRefs)
        songDao.clearSongArtistRefs()
        songDao.insertSongArtistRefs(artistRefs)
    }
}

/** 音源仓库 */
interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun getSource(id: String): Source?
    suspend fun upsert(source: Source)
    suspend fun deleteById(id: String)
}

class RoomSourceRepository constructor(
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

class RoomAlbumRepository constructor(
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

class RoomArtistRepository constructor(
    private val artistDao: ArtistDao,
) : ArtistRepository {
    override fun observeArtists(): Flow<List<Artist>> =
        artistDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeArtistWithSongs(artistId: String): Flow<com.muses.player.core.data.db.ArtistWithSongs?> =
        artistDao.observeArtistWithSongs(artistId)
}

/**
 * 仓库装配（P2a Hilt→Koin：原 `@Module` + `@Binds`）。
 * `@Binds`→`singleOf` + 接口 `single` 委托；同一实现双接口绑定（ErrorLogStore +
 * ErrorLogCrashPersistence）共享同一单例（见 design.md 映射表）。
 */
val repositoryModule = module {

    singleOf(::RoomSongRepository)
    single<SongRepository> { get<RoomSongRepository>() }

    singleOf(::RoomSourceRepository)
    single<SourceRepository> { get<RoomSourceRepository>() }

    singleOf(::RoomAlbumRepository)
    single<AlbumRepository> { get<RoomAlbumRepository>() }

    singleOf(::RoomArtistRepository)
    single<ArtistRepository> { get<RoomArtistRepository>() }

    singleOf(::RoomPlaylistRepository)
    single<PlaylistRepository> { get<RoomPlaylistRepository>() }

    singleOf(::DataStoreSettingsRepository)
    single<SettingsRepository> { get<DataStoreSettingsRepository>() }

    singleOf(::AndroidKeyStoreCredentialsRepository)
    single<CredentialsRepository> { get<AndroidKeyStoreCredentialsRepository>() }

    singleOf(::AndroidKeystoreCryptoEngine)
    single<CryptoEngine> { get<AndroidKeystoreCryptoEngine>() }

    /** 错误日志环形缓冲（任务 08-26-settings-log-viewer） */
    singleOf(::RingBufferErrorLogStore)
    single<ErrorLogStore> { get<RingBufferErrorLogStore>() }

    /** 崩溃持久化能力（CrashHandler 专用，同一实现双接口绑定） */
    single<ErrorLogCrashPersistence> { get<RingBufferErrorLogStore>() }

    /** 播放快照/最近播放（PlaybackService 恢复队列用，P2a 补漏：Hilt 时代靠 @Inject 构造自动提供） */
    singleOf(::PlaybackStateRepository)
    singleOf(::RecentPlaysRepository)
}
