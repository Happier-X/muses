package com.muses.player.feature.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesIconButtonSize
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.icons.TablerIcons
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页（曲库与在线结果）。
 *
 * 交互：输入关键词 → 5 平台并行搜索 → 按平台分组展示 → 点结果直接播放。
 * 部分平台失败不影响其它平台（失败原因就地展示在该平台分组下）。
 */
@Composable
fun OnlineSearchScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    /** 首页搜索框带入的关键词；非空则进入即自动搜索（空串 = 保持旧的无参进入行为） */
    initialKeyword: String = "",
    viewModel: OnlineSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val libraryResults by viewModel.libraryResults.collectAsState()
    val availableQualities by viewModel.availableQualities.collectAsState()
    val preferredQuality by viewModel.preferredQuality.collectAsState()
    val scheme = MiuixTheme.colorScheme

    // 首次进入即汇总脚本声明的音质档位（决定音质行展示哪些选项）
    LaunchedEffect(Unit) { viewModel.refreshAvailableQualities() }

    // 首页带入关键词：预填后立即搜一次。
    // 前置判定 keyword 不一致，除了防重，还兼顾「用户从页内改过关键词后再回退重进」的语义：
    // 路由里带的关键词才是这一刻的意图，页内修改不应被重置。
    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotBlank() && state.keyword != initialKeyword) {
            viewModel.prefillKeyword(initialKeyword)
            viewModel.search()
        }
    }

    Scaffold(
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "搜索",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── 搜索输入行 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MusesTextField(
                    value = state.keyword,
                    onValueChange = viewModel::updateKeyword,
                    modifier = Modifier.weight(1f),
                    label = "搜索歌曲、专辑、艺术家",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                )
                Spacer(Modifier.width(8.dp))
                MusesButton(
                    onClick = { viewModel.search() },
                    enabled = !state.searching,
                ) {
                    Text(if (state.searching) "搜索中" else "搜索")
                }
            }

            // 提示横幅（如「该平台无可用脚本」）：点按可关闭
            state.message?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.errorContainer)
                        .clickable { viewModel.clearMessage() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TablerIcons.Info,
                        contentDescription = null,
                        tint = scheme.onErrorContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = scheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = TablerIcons.Close,
                        contentDescription = "关闭提示",
                        tint = scheme.onErrorContainer,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            when {
                !state.searched -> {
                    MusesEmpty(
                        title = "搜索",
                        description = "输入关键词，搜索曲库中的歌曲、专辑、艺术家及在线歌曲。",
                        icon = TablerIcons.Search,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + com.muses.player.core.ui.theme.LocalBottomChromePadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(key = "library-title") { SearchSectionTitle("曲库") }
                        if (libraryResults.keyword == state.searchedKeyword) {
                            val songs = libraryResults.songs
                            if (songs.isNotEmpty()) {
                                item(key = "songs-title") { SearchSectionTitle("歌曲") }
                                items(songs, key = { "song-${it.id}" }) { song ->
                                    LibraryResultRow(song.title, song.artist.orEmpty(), "播放") {
                                        viewModel.playLibrarySong(song.id, songs)
                                    }
                                }
                            }
                            if (libraryResults.albums.isNotEmpty()) {
                                item(key = "albums-title") { SearchSectionTitle("专辑") }
                                items(libraryResults.albums, key = { "album-${it.id}" }) { album ->
                                    LibraryResultRow(album.title, album.artist.orEmpty(), "${album.songCount} 首") {
                                        onAlbumClick(album.id)
                                    }
                                }
                            }
                            if (libraryResults.artists.isNotEmpty()) {
                                item(key = "artists-title") { SearchSectionTitle("艺术家") }
                                items(libraryResults.artists, key = { "artist-${it.id}" }) { artist ->
                                    LibraryResultRow(artist.name, "${artist.songCount} 首歌曲", "查看") {
                                        onArtistClick(artist.id)
                                    }
                                }
                            }
                            if (songs.isEmpty() && libraryResults.albums.isEmpty() && libraryResults.artists.isEmpty()) {
                                item(key = "library-empty") {
                                    Text("曲库暂无匹配内容", color = scheme.onSurfaceVariantSummary)
                                }
                            }
                        } else {
                            item(key = "library-loading") { CircularProgressIndicator() }
                        }
                        item(key = "online-title") {
                            SearchSectionTitle("在线")
                        }
                        item(key = "platform-filters") {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                PlatformPill("全部", state.filterPlatform == null, { viewModel.setFilter(null) })
                                state.platforms.forEach { platform ->
                                    PlatformPill(
                                        label = platform.displayName,
                                        selected = state.filterPlatform == platform.platform,
                                        onClick = { viewModel.setFilter(platform.platform) },
                                        badge = platform.results.size.takeIf { it > 0 }?.toString(),
                                    )
                                }
                            }
                        }
                        if (availableQualities.isNotEmpty()) {
                            item(key = "qualities") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("音质", fontSize = 12.sp, color = scheme.onSurfaceVariantSummary)
                                    availableQualities.forEach { quality ->
                                        PlatformPill(
                                            quality.label,
                                            preferredQuality == quality.key,
                                            { viewModel.setPreferredQuality(quality.key) },
                                        )
                                    }
                                }
                            }
                            if (LxQuality.fromKey(preferredQuality)?.isHighTier == true) {
                                item(key = "quality-hint") {
                                    Text(
                                        "高音质档取决于音源脚本能力，可能因加密容器/编码不受支持而失败。",
                                        fontSize = 11.sp,
                                        color = scheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }
                        if (!state.searching && state.totalResults == 0) {
                            item(key = "online-empty") {
                                Text("在线暂无匹配内容", color = scheme.onSurfaceVariantSummary)
                            }
                        }
                        state.visiblePlatforms.forEach { platform ->
                            item(key = "hdr-${platform.platform}") {
                                PlatformSectionHeader(platform)
                            }
                            items(
                                items = platform.results,
                                key = { "${platform.platform}-${it.songId}" },
                            ) { result ->
                                val index = platform.results.indexOf(result)
                                SearchResultRow(
                                    result = result,
                                    onClick = { viewModel.playResult(platform.platform, index) },
                                )
                            }
                            if (platform.hasMore) {
                                item(key = "more-${platform.platform}") {
                                    LoadMoreRow(
                                        loading = platform.loadingMore,
                                        onClick = { viewModel.loadMore(platform.platform) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = MiuixTheme.colorScheme.onSurface,
    )
}

@Composable
private fun LibraryResultRow(title: String, subtitle: String, detail: String, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(detail, fontSize = 12.sp, color = scheme.onSurfaceVariantSummary)
        }
    }
}

/** 平台筛选胶囊 */
@Composable
private fun PlatformPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = if (badge != null) "$label $badge" else label,
            fontSize = 13.sp,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 平台分组标题（含加载/错误态） */
@Composable
private fun PlatformSectionHeader(platform: PlatformSearchState) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = platform.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
        )
        when {
            platform.loading -> {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(13.dp))
            }
            platform.error != null -> {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = platform.error,
                    fontSize = 12.sp,
                    color = scheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            platform.results.isNotEmpty() -> {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${platform.results.size} 首",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 单条搜索结果 */
@Composable
private fun SearchResultRow(
    result: OnlineSearchResult,
    onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(result.artist, result.album)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            result.durationMs?.takeIf { it > 0 }?.let { ms ->
                Text(
                    text = formatDuration(ms),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = TablerIcons.Play,
                contentDescription = "播放",
                modifier = Modifier.size(17.dp),
                tint = scheme.primary,
            )
        }
    }
}

/** 加载更多 */
@Composable
private fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
        } else {
            Text(
                text = "加载更多",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 毫秒 → m:ss */
private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}
