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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesTopBar
import top.yukonga.miuix.kmp.squircle.squircleClip

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
    val topBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            MusesTopBar(title = "专辑", largeTitle = "专辑", scrollBehavior = topBarScrollBehavior)
        },
    ) { padding ->
        // __grid：顶栏停靠后内容自顶栏下方起排（玻璃下穿 + 真磨砂随自绘 navbar 退役）
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.album.id }) { card ->
                    // __card：surface-1 圆角卡 + 按压 surface-2
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .squircleClip(12.dp)
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
                                .squircleClip(8.dp)
                                .background(scheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            GridCover(uri = card.coverUri, modifier = Modifier.fillMaxSize())
                        }
                        // __info：标题 + meta
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = card.album.title,
                                style = MiuixTheme.textStyles.main,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = (17 * 1.3).sp,
                                color = scheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.album.songCount} 首歌曲",
                                style = MiuixTheme.textStyles.footnote1,
                                lineHeight = (13 * 1.35).sp,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = card.album.artist ?: "",
                                style = MiuixTheme.textStyles.footnote1,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
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
    val topBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            MusesTopBar(title = "艺术家", largeTitle = "艺术家", scrollBehavior = topBarScrollBehavior)
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(cards, key = { it.artist.id }) { card ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .squircleClip(12.dp)
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
                                style = MiuixTheme.textStyles.main,
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
                                style = MiuixTheme.textStyles.footnote1,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${card.artist.albumCount} 张专辑",
                                style = MiuixTheme.textStyles.footnote1,
                                color = scheme.onBackgroundVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
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
