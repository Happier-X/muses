package com.muses.player.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.muses.player.core.ui.theme.LocalMusesHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.AlbumRepository
import com.muses.player.core.data.repository.ArtistRepository
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 专辑/艺术家页 —— AlbumsPage.vue / ArtistsPage.vue 一比一翻译。
 *
 * 共同结构（BEM 类名见各段注释）：
 * - `.m-navbar`（absolute 玻璃）叠于内容之上，列表滚动从其下穿过；
 * - `.albums-page__grid`：两列网格，gap 16、padding 16、底部 MiniPlayer 留白；
 * - 卡片 `__card`：surface-1 底、radius-card 圆角、padding sub(12)、按压 surface-2；
 *   封面满宽 1:1（专辑 radius-sm / 艺术家圆形）、标题 17/600 两行省略、
 *   meta 13px text2 单行省略。
 */

// ---------------------------------------------------------------------------
// ViewModel：合并条目与封面映射
// ---------------------------------------------------------------------------

data class AlbumCard(val album: Album, val coverUri: String?)

@HiltViewModel
class AlbumCardsViewModel @Inject constructor(
    albumRepository: AlbumRepository,
    albumDao: com.muses.player.core.data.dao.AlbumDao,
) : ViewModel() {
    // Hilt 禁止 ViewModel 互注入：这里自行组合专辑列表与封面（数据口径同 AlbumsViewModel）
    val cards: StateFlow<List<AlbumCard>> = combine(
        albumRepository.observeAlbums(),
        albumDao.observeAlbumCovers(),
    ) { albums, covers ->
        val coverMap = covers.associate { it.albumId to it.coverUri }
        albums.map { AlbumCard(it, coverMap[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ArtistCard(val artist: Artist, val coverUri: String?)

@HiltViewModel
class ArtistCardsViewModel @Inject constructor(
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

// ---------------------------------------------------------------------------
// 专辑页
// ---------------------------------------------------------------------------

@Composable
fun AlbumsPage(
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumCardsViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val cards by viewModel.cards.collectAsState()
    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalMusesHazeState provides hazeState) {
        Box(modifier.fillMaxSize()) {
        // __grid：滚动区域从玻璃 navbar 下穿过（Web 版 absolute navbar 同观感）—— 真磨砂：网格为 hazeSource
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentAlignment = Alignment.Center,
            ) {
                SaltEmpty(title = "还没有专辑", description = "请先到音源页添加并扫描音源。")
            }
        } else {
            LazyVerticalGrid(
                // Web ≥768px：repeat(auto-fill, minmax(180px, 1fr))；手机恒两列
                columns = if (isTabletWidth()) GridCells.Adaptive(180.dp) else GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = gridTopPadding(),
                    bottom = 96.dp, // --m-content-pb（MiniPlayer 留白）
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.album.id }) { card ->
                    // __card：surface-1 圆角卡 + 按压 surface-2
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SaltRadius.card))
                            .background(salt.surface1)
                            .clickable { onAlbumClick(card.album.id) }
                            .padding(SaltSpacing.spacingSub),
                        verticalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
                    ) {
                        // __cover：满宽 1:1（覆盖 MCover 固定尺寸），radius-sm
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(SaltRadius.sm))
                                .background(salt.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            GridCover(uri = card.coverUri, modifier = Modifier.fillMaxSize())
                        }
                        // __info：标题 + meta
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = card.album.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = (17 * 1.3).sp,
                                color = salt.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.album.songCount} 首歌曲",
                                fontSize = 13.sp,
                                lineHeight = (13 * 1.35).sp,
                                color = salt.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = card.album.artist ?: "",
                                fontSize = 13.sp,
                                color = salt.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // navbar 叠加层（玻璃）—— 真磨砂由 SaltNavbar 内部 hazeEffect 消费
        SaltNavbar(title = "专辑")
        }
    }
}

// ---------------------------------------------------------------------------
// 艺术家页
// ---------------------------------------------------------------------------

@Composable
fun ArtistsPage(
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistCardsViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val cards by viewModel.cards.collectAsState()
    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalMusesHazeState provides hazeState) {
        Box(modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentAlignment = Alignment.Center,
            ) {
                SaltEmpty(title = "还没有艺术家", description = "请先到音源页添加并扫描音源。")
            }
        } else {
            LazyVerticalGrid(
                // 同专辑页：Web ≥768px auto-fill minmax(180px, 1fr)
                columns = if (isTabletWidth()) GridCells.Adaptive(180.dp) else GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = gridTopPadding(),
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.artist.id }) { card ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SaltRadius.card))
                            .background(salt.surface1)
                            .clickable { onArtistClick(card.artist.id) }
                            .padding(SaltSpacing.spacingSub),
                        verticalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
                    ) {
                        // __cover：圆形（艺术家特有）
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(salt.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            GridCover(uri = card.coverUri, modifier = Modifier.fillMaxSize())
                        }
                        // __info：居中排版（艺术家特有）
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = card.artist.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = (17 * 1.3).sp,
                                color = salt.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${card.artist.songCount} 首歌曲",
                                fontSize = 13.sp,
                                color = salt.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.artist.albumCount} 张专辑",
                                fontSize = 13.sp,
                                color = salt.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        SaltNavbar(title = "艺术家")
        }
    }
}

/**
 * 网格卡片封面：满宽裁切图；无封面时 surface-2 底 + 音符占位
 * （对照 Web 版 getAlbumCoverSrc 空值回退 + MCover 占位）。
 */
@Composable
private fun GridCover(uri: String?, modifier: Modifier = Modifier) {
    val salt = LocalSaltColors.current
    Box(modifier.background(salt.surface2), contentAlignment = Alignment.Center) {
        if (uri != null) {
            coil3.compose.AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = salt.text2,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 内容区顶部避让：navbar-pt + 44px 内容行（`.albums-page__content` 公式） */
@Composable
private fun gridTopPadding(): Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusBarTop = with(density) {
        androidx.compose.foundation.layout.WindowInsets.statusBars
            .getTop(this).toDp()
    }
    return statusBarTop.coerceAtLeast(16.dp) + 44.dp
}

/**
 * Web 断点口径：viewport 宽 ≥768 即平板形态（TabsPage.vue isTablet 同款判定）。
 * 页面级用屏幕宽度而非容器宽度——与 Web media query 的 viewport 口径一致。
 */
@Composable
private fun isTabletWidth(): Boolean =
    LocalConfiguration.current.screenWidthDp >= 768
