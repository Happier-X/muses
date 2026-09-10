package com.muses.player.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import androidx.compose.ui.unit.dp
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.db.SongTags
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.playback.PlaybackMeta
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.ui.components.MiniPlayerBar
import com.muses.player.feature.shell.platform.PermissionsEffect
import com.muses.player.feature.shell.platform.ShellBackHandler
import com.muses.player.feature.shell.platform.smartBottomBarInsetsPadding
import com.muses.player.feature.library.AlbumDetailScreen
import com.muses.player.feature.library.AlbumsPage
import com.muses.player.feature.library.ArtistDetailScreen
import com.muses.player.feature.library.ArtistsPage
import com.muses.player.feature.library.SongsPage
import com.muses.player.feature.player.PlayerScreen
import com.muses.player.feature.player.QueueScreen
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.playlist.PlaylistDetailPage
import com.muses.player.feature.playlist.PlaylistsPage
import com.muses.player.feature.sources.SourcesScreen
import com.muses.player.feature.sources.WebDavBrowseScreen
import com.muses.player.feature.sources.WebDavFormScreen
import com.muses.player.settings.SettingsScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** MiniPlayerBar 的数据快照（对照 MiniPlayer.vue 的 playerState.currentSong 消费口径） */
data class NowPlayingUiState(
    val title: String,
    /** 宽屏副标题（≥ [TabletBreakpoint]）：「{artist} - {album}」，空值回退「未知艺术家/未知专辑」 */
    val subtitle: String,
    /** 窄屏副标题：只显示艺术家（专辑不进迷你条，09-07 定案） */
    val artist: String,
    val coverUri: String?,
)

/**
 * 主界面 ViewModel：管理播放连接与 MiniPlayer 数据（U22 上收 commonMain，
 * 播放链经 [PlaybackPort] 端口驱动，曲库标签合并逻辑保持不变）。
 */
class MainViewModel constructor(
    private val playback: PlaybackPort,
    private val songDao: SongDao,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playback.isPlaying

    /**
     * 当前曲信息：优先用 Room 的已回写标签（lazyScan 后），未回写前回退到播放栈实时
     * 元数据（[PlaybackMeta]，通知栏同源，解决「通知栏有信息但底部栏仍未知」）。
     * null = 无当前曲（MiniPlayer 显示空态整条，对照 `.mini-player--empty`）。
     */
    val nowPlaying: StateFlow<NowPlayingUiState?> = combine(
        playback.currentSongId,
        playback.currentMeta,
    ) { songId, meta -> songId to meta }
        .flatMapLatest { (songId, meta) ->
            if (songId == null) {
                flowOf(null)
            } else {
                // 修复：改为 observeById Flow，使播放时回写后迷你条与列表一致（原 one-shot 不响应 DB 更新）
                songDao.observeById(songId).map { song -> mergeNowPlaying(songId, song, meta) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── 迷你条歌词模式 ──

    /** 开关状态 */
    val miniPlayerLyricsEnabled: StateFlow<Boolean> = settingsRepository.miniPlayerLyricsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 当前歌词行文本（null = 无歌词或开关关闭） */
    private val _currentLyricLine = MutableStateFlow<String?>(null)
    val currentLyricLine: StateFlow<String?> = _currentLyricLine.asStateFlow()

    /** 已解析歌词文档（切歌时更新） */
    private var lyricsDocument: LyricsDocument? = null

    init {
        observeLyrics()
        startLyricPositionPolling()
    }

    /** 订阅当前曲歌词变化：切歌时解析歌词文本 → 更新 [lyricsDocument] */
    private fun observeLyrics() {
        viewModelScope.launch {
            playback.currentSongId
                .flatMapLatest { songId ->
                    if (songId == null) flowOf(null)
                    else songDao.observeById(songId)
                }
                .collect { songEntity ->
                    val doc = withContext(Dispatchers.Default) {
                        LyricsParser.parseDocument(songEntity?.lyrics)
                    }
                    lyricsDocument = doc
                }
        }
    }

    /** 歌词进度轮询：~100ms，根据播放位置查找当前歌词行 */
    private fun startLyricPositionPolling() {
        viewModelScope.launch {
            while (true) {
                if (miniPlayerLyricsEnabled.value) {
                    val doc = lyricsDocument
                    val pos = playback.currentPosition()
                    val index = doc?.highlightedIndex(pos)
                    _currentLyricLine.value = index?.let { doc?.lines?.getOrNull(it)?.text }
                } else {
                    _currentLyricLine.value = null
                }
                delay(100)
            }
        }
    }

    private fun mergeNowPlaying(
        songId: String?,
        song: com.muses.player.core.data.db.SongEntity?,
        meta: PlaybackMeta?,
    ): NowPlayingUiState? {
        if (songId == null) return null
        val metaTitle = meta?.title?.trim()?.takeIf { it.isNotEmpty() }
        val metaArtist = meta?.artist?.trim()?.takeIf { it.isNotEmpty() }
        val metaAlbum = meta?.album?.trim()?.takeIf { it.isNotEmpty() }
        val metaCover = meta?.coverUri
        // 修复：已刮削字段（metaTitle/metaArtist/metaAlbum/metaCover 非空）优先库值，避免重刮削后仍显示旧文件标签
        val title = when {
            song == null -> metaTitle ?: "未知歌曲"
            song.metaTitle != null -> song.title
            song.tagsVersion < SongTags.TAGS_VERSION -> metaTitle ?: song.title
            else -> song.title
        }
        val artist = when {
            song == null -> metaArtist ?: "未知艺术家"
            song.metaArtist != null -> song.artist ?: "未知艺术家"
            song.tagsVersion < SongTags.TAGS_VERSION -> metaArtist ?: song.artist ?: "未知艺术家"
            else -> song.artist ?: "未知艺术家"
        }
        val album = when {
            song == null -> metaAlbum ?: "未知专辑"
            song.metaAlbum != null -> song.albumTitle ?: "未知专辑"
            song.tagsVersion < SongTags.TAGS_VERSION -> metaAlbum ?: song.albumTitle ?: "未知专辑"
            else -> song.albumTitle ?: "未知专辑"
        }
        val cover = when {
            song == null -> metaCover
            song.metaCover != null -> song.coverUri
            song.tagsVersion < SongTags.TAGS_VERSION -> metaCover ?: song.coverUri
            else -> song.coverUri
        }
        return NowPlayingUiState(
            title = title,
            subtitle = "$artist - $album",
            artist = artist,
            coverUri = cover,
        )
    }

    /** 存量库专辑/艺术家索引回填（幂等） */
    fun rebuildLibraryIndexes() {
        viewModelScope.launch {
            runCatching { songRepository.rebuildDerivedIndexes() }
        }
    }

    fun connectPlayer() {
        playback.connect()
    }

    fun disconnectPlayer() {
        playback.disconnect()
    }

    fun playPause() = playback.playPause()

    fun skipToNext() = playback.skipToNext()

    fun skipToPrevious() = playback.skipToPrevious()
}

/**
 * 主框架入口（U22 双端共享）—— P1 复刻版：TabsLayout 双形态导航（aside/drawer）+
 * CMP Navigation NavHost + MiniPlayer 叠加。原 M1 的 ModalNavigationDrawer/Scaffold/
 * TopAppBar 骨架已由 TabsPage.vue 对照实现整体替换。
 */
@Composable
fun MusesApp() {
    // miuix-nav 返回栈（类型化路由，存栈恢复经 kotlinx.serialization；替代 CMP Navigation）
    val backStack = rememberNavBackStack<MusesRoute>(MusesRoute.Songs)
    // 当前栈顶（SnapshotStateList 读取即订阅，路由变化自动重组；对照原 currentBackStackEntryAsState）
    val currentKey = backStack.lastOrNull()

    val viewModel: MainViewModel = koinViewModel()

    // 连接播放服务
    LaunchedEffect(Unit) {
        viewModel.connectPlayer()
        // 存量库回填：albums/artists 索引此前无维护方，启动时幂等重建一次
        viewModel.rebuildLibraryIndexes()
    }

    // 权限申请（安卓：READ_MEDIA_AUDIO/POST_NOTIFICATIONS；桌面空实现）
    PermissionsEffect()

    // S3 待审队列宿主 VM（MusesApp 作用域持有，review 回调直用；原按回退栈取 Scrape 页 entry 实例）
    val scrapeVm: com.muses.player.feature.scrape.ScrapeViewModel = koinViewModel()

    // 沉浸式/队列改为状态驱动的 overlay（Box 叠加于 Tabs 之上），下滑漏出背后列表而非纯黑窗口
    var showPlayerOverlay by remember { mutableStateOf(false) }
    var showQueueOverlay by remember { mutableStateOf(false) }

    // 导航项组装
    val primaryItems = NavDestination.Primary.map { dest -> dest.toNavItem(currentKey, backStack) }
    val secondaryItems = NavDestination.Secondary.map { dest -> dest.toNavItem(currentKey, backStack) }
    // 窄屏底部导航 4 栏：曲库（= 歌曲页）/音源/刮削/设置；首页（专辑/艺术家/歌单入口）后续加入即 5 栏
    val bottomItems = listOf(
        NavDestination.Songs.toNavItem(currentKey, backStack).copy(label = "曲库"),
        NavDestination.Sources.toNavItem(currentKey, backStack),
        NavDestination.Scrape.toNavItem(currentKey, backStack),
        NavDestination.Settings.toNavItem(currentKey, backStack),
    )

    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val miniPlayerLyricsEnabled by viewModel.miniPlayerLyricsEnabled.collectAsState()
    val currentLyricLine by viewModel.currentLyricLine.collectAsState()

    // 底部导航槽位化：根 Scaffold bottomBar = 迷你条（上）+ 窄屏导航栏（下）。
    // 导航（aside/底栏）与内容叠层顺序由 Scaffold 统一保证，导航栏不再进 body，避免被 dock 盖住。
    // 断点与 TabsLayout 同口径（BoxWithConstraints 视口宽 ≥768dp 即平板）。
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTabletBar = maxWidth >= TabletBreakpoint
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(Modifier.fillMaxWidth()) {
                    BoxWithConstraints(Modifier.smartBottomBarInsetsPadding()) {
                // 歌词模式：开关开启且有当前歌词行时，用歌词替换艺术家
                val lyricLine = if (miniPlayerLyricsEnabled) currentLyricLine else null
                val miniSubtitle = if (lyricLine != null) {
                    lyricLine
                } else if (maxWidth >= TabletBreakpoint) {
                    // 宽屏（Windows/平板）「艺术家 - 专辑」
                    nowPlaying?.subtitle ?: "未知艺术家 - 未知专辑"
                } else {
                    // 窄屏「艺术家」
                    nowPlaying?.artist ?: "未知艺术家"
                }
                MiniPlayerBar(
                    title = nowPlaying?.title ?: "暂无播放歌曲",
                    subtitle = miniSubtitle,
                    coverUri = nowPlaying?.coverUri,
                    isPlaying = isPlaying,
                    hasSong = nowPlaying != null,
                    onOpenPlayer = { showPlayerOverlay = true },
                    onTogglePlayback = { viewModel.playPause() },
                    onOpenQueue = { showQueueOverlay = true },
                    onNext = { viewModel.skipToNext() },
                    onPrevious = { viewModel.skipToPrevious() },
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                )
                }
                // 悬浮胶囊底栏（官方 FloatingNavigationBar，图标-only，label 进无障碍文案）：
                // 外边距 18dp 与上方迷你条左右对齐；系统导航条边衬已由迷你条承担，
                // 此处 defaultWindowInsetsPadding = false 避免双重留白
                if (!isTabletBar) {
                    FloatingNavigationBar(
                        horizontalOutSidePadding = 18.dp,
                        defaultWindowInsetsPadding = false,
                    ) {
                        bottomItems.forEach { item ->
                            FloatingNavigationBarItem(
                                selected = item.active,
                                onClick = item.onClick,
                                icon = item.icon,
                                label = item.label,
                            )
                        }
                    }
                }
            }
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
        // 结构恒定：overlay 打开时不得切换 TabsLayout 分支（navVisible 恒 true）——
        // 原版 Web overlay 打开仅锁主页面交互（pointer-events:none），DOM 全保留；
        // 之前按 overlayRoute 切 navVisible 会让 content（NavHost）在组合树换位销毁重建，
        // 底下列表停止绘制 → 下滑沉浸页露出纯黑（08-28 下滑露黑根因）。
        TabsLayout(
            primaryItems = primaryItems,
            secondaryItems = secondaryItems,
            navVisible = true,
        ) {
            AppNavHost(backStack, scrapeVm)
        }
        }
    }

    // 沉浸式 overlay 与 Scaffold 平级（BoxWithConstraints 双子项）：Scaffold 的 bottomBar
    // （MiniPlayerBar + 悬浮导航栏）绘制层级在 body 之上，overlay 若留在 body 内会被
    // 白色迷你条/导航栏盖住沉浸页底部控件（MuMu 实测）；平级后 overlay 必然盖过
    // bottomBar，恢复「全屏盖住不可见、不可交互」的预期层级。
    if (showPlayerOverlay) {
        ShellBackHandler { showPlayerOverlay = false }
        Box(Modifier.fillMaxSize()) {
            val playerVm: com.muses.player.feature.player.PlayerViewModel = koinViewModel()
            // U12：当前曲改由曲库实时流（SongEntity→领域模型），原 MediaItem 手拼字段等价
            val currentSong by playerVm.currentSong.collectAsState()
            var showEditMeta by remember { mutableStateOf(false) }
            if (showEditMeta) {
                val editSong = currentSong?.toDomain()
                com.muses.player.feature.scrape.EditMetaSheet(song = editSong, onDismiss = { showEditMeta = false })
            }
            PlayerScreen(
                onClose = { showPlayerOverlay = false },
                onOpenQueue = { showPlayerOverlay = false; showQueueOverlay = true },
                onOpenEditMeta = { showEditMeta = true },
            )
        }
    }
    if (showQueueOverlay) {
        // 队列已是 Dialog 弹窗：返回/遮罩点击由 Dialog 自行消费，此处不再套 ShellBackHandler
        QueueScreen(onClose = { showQueueOverlay = false })
    }
    }
}

/** 导航项组装（图标/文案/激活判定均来自 NavDestination 的 Web 层映射） */
private fun NavDestination.toNavItem(
    currentKey: NavKey?,
    backStack: NavBackStack,
): MusesNavItem = MusesNavItem(
    icon = icon,
    label = label,
    active = isActive(currentKey),
    onClick = { navigateToTab(backStack, this) },
)

/**
 * 幂等 push：栈顶即目标则不动（单例），否则先移除同值 key 再压栈。
 * miuix-nav 同值重复 push 会在 reconcile 直接抛 IllegalArgumentException，
 * 此处顺带承担连点防抖（对照官方指南「push 幂等」要求）。
 */
private fun NavBackStack.pushUnique(key: NavKey) {
    if (lastOrNull() == key) return
    remove(key)
    add(key)
}

/** 回退：仅栈深 >1 时弹栈（miuix-nav 空栈非法；对照原 popBackStack 布尔语义）。 */
private fun NavBackStack.pop() {
    if (size > 1) removeAt(lastIndex)
}

/**
 * 切 tab：清到 Songs 根后按需 push。
 * 对照原 popUpTo(Songs){saveState} + launchSingleTop + restoreState；
 * per-tab 状态不跨次保留（列表由 flow 重载，滚动回到顶部），详情返回栈不保留。
 */
private fun navigateToTab(backStack: NavBackStack, destination: NavDestination) {
    if (destination == NavDestination.Songs) {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        return
    }
    while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    backStack.pushUnique(destination.routeKey)
}

@Composable
private fun AppNavHost(
    backStack: NavBackStack,
    /** S3 待审队列宿主 VM（MusesApp 作用域持有；原按回退栈取 Scrape 页 entry 实例） */
    scrapeVm: com.muses.player.feature.scrape.ScrapeViewModel,
) {
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
    ) {
        entry<MusesRoute.Songs> {
            // U16：SongsPage 已上收 commonMain，经端口消费（不再依赖 Media3 具体类）
            val playback = org.koin.compose.koinInject<com.muses.player.core.playback.PlaybackPort>()
            // M3：刮削队列入队（ScrapeQueueStore 为 @Singleton，经 koinViewModel 载体注入）
            val scrapeQueueVm: com.muses.player.feature.scrape.ScrapeQueueAccessViewModel = koinViewModel()
            SongsPage(
                playback = playback,
                onEnqueueScrape = { ids -> scrapeQueueVm.enqueue(ids) },
            )
        }
        entry<MusesRoute.Albums> {
            AlbumsPage(
                onAlbumClick = { albumId ->
                    backStack.pushUnique(MusesRoute.AlbumDetail(albumId))
                },
            )
        }
        entry<MusesRoute.Artists> {
            ArtistsPage(
                onArtistClick = { artistId ->
                    backStack.pushUnique(MusesRoute.ArtistDetail(artistId))
                },
            )
        }
        entry<MusesRoute.AlbumDetail>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            val playerConnection = koinViewModel<com.muses.player.feature.player.PlayerViewModel>().playback
            AlbumDetailScreen(
                albumId = route.albumId,
                onBack = { backStack.pop() },
                // U9：播放连接经回调注入（详情屏已上收 commonMain，不再依赖 core:media）
                onPlaySong = { songId, songs -> playerConnection.play(songId, songs) },
            )
        }
        entry<MusesRoute.ArtistDetail>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            val playerConnection = koinViewModel<com.muses.player.feature.player.PlayerViewModel>().playback
            ArtistDetailScreen(
                artistId = route.artistId,
                onBack = { backStack.pop() },
                onPlaySong = { songId, songs -> playerConnection.play(songId, songs) },
            )
        }
        entry<MusesRoute.Playlists> {
            PlaylistsPage(onOpenPlaylist = { playlistId ->
                backStack.pushUnique(MusesRoute.PlaylistDetail(playlistId))
            })
        }
        entry<MusesRoute.PlaylistDetail>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            PlaylistDetailPage(
                playlistId = route.playlistId,
                onBack = { backStack.pop() },
            )
        }
        // 刮削页随 M3 复刻（VM 由宿主直持传入；原 ScrapeScreen 内 koinViewModel() entry 作用域已改显式注入）
        entry<MusesRoute.Scrape> {
            com.muses.player.feature.scrape.ScrapeScreen(
                viewModel = scrapeVm,
                onOpenReview = { songId ->
                    backStack.pushUnique(MusesRoute.ScrapeReview(songId))
                },
                // S3 逐首审核：带 queue 上下文进入审核页（「应用并下一首」推进用）
                onStartReviewQueue = { firstSongId, queue ->
                    backStack.pushUnique(
                        MusesRoute.ScrapeReview(firstSongId, queue.joinToString(",")),
                    )
                },
            )
        }
        // 单曲刮削审核页（Tagger 式就地审核，design §2.1）
        entry<MusesRoute.ScrapeReview>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            // 路由直传参数经 Koin parametersOf 供给 VM（miuix-nav 无 SavedStateHandle）
            val reviewVm: com.muses.player.feature.scrape.ScrapeReviewViewModel = koinViewModel(
                parameters = { parametersOf(route.songId, route.queueCsv) },
            )
            com.muses.player.feature.scrape.ScrapeReviewScreen(
                viewModel = reviewVm,
                onBack = { backStack.pop() },
                // S3「应用并下一首」：同步预览（剔除已写回者）→ 推进待审队列 → 打开下一首（结束则返回）
                onAppliedAndNext = { writtenSongId, _ ->
                    scrapeVm.refreshAfterExternalWriteback(writtenSongId)
                    val next = scrapeVm.advanceReview(writtenSongId)
                    if (next != null) {
                        val rest = scrapeVm.pendingReviewQueue.value.orEmpty()
                        // 对照原 popUpTo(REVIEW){inclusive} + navigate：同值替换（entry 重建，VM 随之重建）
                        backStack.remove(route)
                        backStack.add(MusesRoute.ScrapeReview(next, rest.joinToString(",")))
                    } else {
                        backStack.pop()
                    }
                },
                // S3 手动返回：清待审队列，不强推下一首
                onManualBack = { scrapeVm.cancelReviewQueue() },
            )
        }
        entry<MusesRoute.Sources> {
            SourcesScreen(
                onOpenWebdavAdd = { backStack.pushUnique(MusesRoute.WebDavAdd) },
                onOpenWebdavEdit = { sourceId ->
                    backStack.pushUnique(MusesRoute.WebDavEdit(sourceId))
                },
            )
        }
        entry<MusesRoute.WebDavAdd>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            WebDavFormScreen(
                sourceId = null,
                onBack = { backStack.pop() },
                // 连接信息直传类型化字段（原 URLEncoder query 入参；含密码，不落日志）
                onBrowse = { mode, initialPath, serverUrl, username, password ->
                    backStack.pushUnique(
                        MusesRoute.WebDavBrowse(mode, initialPath, serverUrl, username, password),
                    )
                },
            )
        }
        entry<MusesRoute.WebDavEdit>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            WebDavFormScreen(
                sourceId = route.sourceId,
                onBack = { backStack.pop() },
                onBrowse = { mode, initialPath, serverUrl, username, password ->
                    backStack.pushUnique(
                        MusesRoute.WebDavBrowse(mode, initialPath, serverUrl, username, password),
                    )
                },
            )
        }
        entry<MusesRoute.WebDavBrowse>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            WebDavBrowseScreen(
                mode = route.mode,
                initialPath = route.initialPath,
                serverUrl = route.serverUrl,
                username = route.username,
                password = route.password,
                onBack = { backStack.pop() },
                onConfirm = { _ ->
                    // 结果已由浏览页写入 WebDavBrowseResultHolder，这里只回退
                    backStack.pop()
                },
            )
        }
        entry<MusesRoute.Settings> { SettingsScreen() }
    }
}
