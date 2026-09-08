package com.muses.player.feature.library

import top.yukonga.miuix.kmp.theme.MiuixTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.muses.player.core.ui.theme.LocalHazeBlurState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesNavbar

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
// ViewModel（U9 上收 commonMain：AlbumCards/ArtistCards 已移至 LibraryViewModels.kt，
// 与 libraryModule 的注册同处一个 sourceSet）
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 专辑页
// ---------------------------------------------------------------------------

@Composable
fun AlbumsPage(
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumCardsViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val cards by viewModel.cards.collectAsState()
    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalHazeBlurState provides hazeState) {
        Box(modifier.fillMaxSize()) {
        // __grid：滚动区域从玻璃 navbar 下穿过（Web 版 absolute navbar 同观感）—— 真磨砂：网格为 hazeSource
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentAlignment = Alignment.Center,
            ) {
                MusesEmpty(title = "还没有专辑", description = "请先到音源页添加并扫描音源。")
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .clickable { onAlbumClick(card.album.id) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // __cover：满宽 1:1（覆盖 MCover 固定尺寸），radius-sm
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(scheme.surfaceVariant),
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
                                color = scheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.album.songCount} 首歌曲",
                                fontSize = 13.sp,
                                lineHeight = (13 * 1.35).sp,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = card.album.artist ?: "",
                                fontSize = 13.sp,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // navbar 叠加层（玻璃）—— 真磨砂由 MusesNavbar 内部 hazeEffect 消费
        MusesNavbar(title = "专辑")
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
    viewModel: ArtistCardsViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val cards by viewModel.cards.collectAsState()
    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalHazeBlurState provides hazeState) {
        Box(modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentAlignment = Alignment.Center,
            ) {
                MusesEmpty(title = "还没有艺术家", description = "请先到音源页添加并扫描音源。")
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .clickable { onArtistClick(card.artist.id) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // __cover：圆形（艺术家特有）
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(scheme.surfaceVariant),
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
                                color = scheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${card.artist.songCount} 首歌曲",
                                fontSize = 13.sp,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.artist.albumCount} 张专辑",
                                fontSize = 13.sp,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        MusesNavbar(title = "艺术家")
        }
    }
}

/**
 * 网格卡片封面：满宽裁切图；无封面时 surface-2 底 + 音符占位
 * （对照 Web 版 getAlbumCoverSrc 空值回退 + MCover 占位）。
 */
@Composable
private fun GridCover(uri: String?, modifier: Modifier = Modifier) {
    val scheme = MiuixTheme.colorScheme
    Box(modifier.background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (uri != null) {
            coil3.compose.AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                TablerIcons.MusicNote,
                contentDescription = null,
                tint = scheme.onBackgroundVariant,
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
private fun isTabletWidth(): Boolean {
    // U16：LocalConfiguration 为安卓专属，改 LocalWindowInfo 容器宽度（双端一致）
    val containerWidth = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width
    return containerWidth / androidx.compose.ui.platform.LocalDensity.current.density >= 768f
}
