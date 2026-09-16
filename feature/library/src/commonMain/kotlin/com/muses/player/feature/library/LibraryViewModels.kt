package com.muses.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.muses.player.core.data.db.AlbumWithSongs
import com.muses.player.core.data.db.ArtistWithSongs
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.repository.AlbumRepository
import com.muses.player.core.data.repository.ArtistRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 歌曲列表 ViewModel */
class SongsViewModel constructor(
    private val songRepository: SongRepository,
    private val songDao: com.muses.player.core.data.dao.SongDao,
) : ViewModel() {

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")

    // 数据库搜索流：防抖后走 Room 模糊匹配，大库不全量进内存过滤
    @OptIn(FlowPreview::class)
    val songs: StateFlow<List<Song>> = _searchQuery
        .debounce(300).distinctUntilChanged()
        .flatMapLatest { query -> songRepository.observeSongs(query) }
        .flowOn(Dispatchers.Default).distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** 批量删除歌曲（Room 外键 CASCADE 同步清理播放列表引用，语义对齐 653e466） */
    fun deleteByIds(ids: Collection<String>) {
        viewModelScope.launch {
            var failed = 0
            for (id in ids) {
                runCatching { songDao.deleteById(id) }.onFailure { failed++ }
            }
            if (failed > 0) {
                // 聚合失败数，调用方经 Snackbar 提示（避免静默丢失败）
                _deleteErrors.value = failed
            }
        }
    }

    private val _deleteErrors = kotlinx.coroutines.flow.MutableStateFlow(0)
    val deleteErrors: StateFlow<Int> = _deleteErrors

    /** 已展示失败提示后清零，避免重组重复提示 */
    fun consumeDeleteErrors() {
        _deleteErrors.value = 0
    }
}

/** 专辑列表 ViewModel */
class AlbumsViewModel constructor(
    albumRepository: AlbumRepository,
    albumDao: com.muses.player.core.data.dao.AlbumDao,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = albumRepository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 专辑封面（albumId → 首个可用 coverUri），供网格卡片使用 */
    val covers: StateFlow<Map<String, String>> = albumDao.observeAlbumCovers()
        .map { list -> list.associate { it.albumId to it.coverUri } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

/** 专辑详情 ViewModel */
class AlbumDetailViewModel constructor(
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _albumId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val albumWithSongs: StateFlow<AlbumWithSongs?> = _albumId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null)
            else albumRepository.observeAlbumWithSongs(id)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(albumId: String) {
        if (_albumId.value != albumId) {
            _albumId.value = albumId
        }
    }
}

/** 艺术家列表 ViewModel */
class ArtistsViewModel constructor(
    artistRepository: ArtistRepository,
    artistDao: com.muses.player.core.data.dao.ArtistDao,
) : ViewModel() {

    val artists: StateFlow<List<Artist>> = artistRepository.observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 艺术家封面（artistId → 首个可用 coverUri） */
    val covers: StateFlow<Map<String, String>> = artistDao.observeArtistCovers()
        .map { list -> list.associate { it.artistId to it.coverUri } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

/** 艺术家详情 ViewModel */
class ArtistDetailViewModel constructor(
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _artistId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val artistWithSongs: StateFlow<ArtistWithSongs?> = _artistId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null)
            else artistRepository.observeArtistWithSongs(id)
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(artistId: String) {
        if (_artistId.value != artistId) {
            _artistId.value = artistId
        }
    }
}

// ── 卡片 ViewModel（U9 从 LibraryGridPages 上收：libraryModule 注册同源集）──

data class AlbumCard(val album: Album, val coverUri: String?)

class AlbumCardsViewModel constructor(
    albumRepository: AlbumRepository,
    albumDao: com.muses.player.core.data.dao.AlbumDao,
) : ViewModel() {
    // ViewModel 间禁止互注入：这里自行组合专辑列表与封面（数据口径同 AlbumsViewModel）
    val cards: StateFlow<List<AlbumCard>> = combine(
        albumRepository.observeAlbums(),
        albumDao.observeAlbumCovers(),
    ) { albums, covers ->
        val coverMap = covers.associate { it.albumId to it.coverUri }
        albums.map { AlbumCard(it, coverMap[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ArtistCard(val artist: Artist, val coverUri: String?)

class ArtistCardsViewModel constructor(
    artistRepository: ArtistRepository,
    artistDao: com.muses.player.core.data.dao.ArtistDao,
) : ViewModel() {
    val cards: StateFlow<List<ArtistCard>> = combine(
        artistRepository.observeArtists(),
        artistDao.observeArtistCovers(),
    ) { artists, covers ->
        val coverMap = covers.associate { it.artistId to it.coverUri }
        artists.map { ArtistCard(it, coverMap[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
