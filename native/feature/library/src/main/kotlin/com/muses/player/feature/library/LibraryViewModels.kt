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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 歌曲列表 ViewModel */
@HiltViewModel
class SongsViewModel @Inject constructor(
    songRepository: SongRepository,
    private val songDao: com.muses.player.core.data.dao.SongDao,
) : ViewModel() {

    private val _allSongs: StateFlow<List<Song>> = songRepository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")

    val songs: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
                song.artist.orEmpty().contains(query, ignoreCase = true) ||
                song.album.orEmpty().contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** 批量删除歌曲（Room 外键 CASCADE 同步清理播放列表引用，语义对齐 653e466） */
    fun deleteByIds(ids: Collection<String>) {
        viewModelScope.launch {
            ids.forEach { runCatching { songDao.deleteById(it) } }
        }
    }
}

/** 专辑列表 ViewModel */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    albumRepository: AlbumRepository,
) : ViewModel() {

    val albums: StateFlow<List<Album>> = albumRepository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** 专辑详情 ViewModel */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
) : ViewModel() {

    private val _albumId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val albumWithSongs: StateFlow<AlbumWithSongs?> = _albumId
        .combine(albumRepository.observeAlbums()) { id, _ -> id }
        .map { id -> id?.let { albumRepository.observeAlbumWithSongs(it) } }
        .map { flow -> flow?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null) }
        .map { it?.value }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(albumId: String) {
        if (_albumId.value != albumId) {
            _albumId.value = albumId
        }
    }
}

/** 艺术家列表 ViewModel */
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    artistRepository: ArtistRepository,
) : ViewModel() {

    val artists: StateFlow<List<Artist>> = artistRepository.observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** 艺术家详情 ViewModel */
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
) : ViewModel() {

    private val _artistId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val artistWithSongs: StateFlow<ArtistWithSongs?> = _artistId
        .combine(artistRepository.observeArtists()) { id, _ -> id }
        .map { id -> id?.let { artistRepository.observeArtistWithSongs(it) } }
        .map { flow -> flow?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null) }
        .map { it?.value }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(artistId: String) {
        if (_artistId.value != artistId) {
            _artistId.value = artistId
        }
    }
}
