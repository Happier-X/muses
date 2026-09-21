package com.muses.player.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import top.yukonga.miuix.kmp.squircle.squircleBackground
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import com.muses.player.core.ui.components.LocalPlayerAnimatedVisibilityScope
import com.muses.player.core.ui.components.LocalPlayerArtworkKey
import com.muses.player.core.ui.components.LocalPlayerSharedTransitionScope
import com.muses.player.core.ui.components.MusesBottomDock
import com.muses.player.core.ui.components.MusesBottomDockItem
import com.muses.player.core.ui.components.PlayerArtworkSharedKey
import com.muses.player.core.ui.components.MusesDockActionPill
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHostState
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
import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.lyrics.matchDocument
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.LxLyricParser
import com.muses.player.core.model.Song
import com.muses.player.core.model.online.NoOpOnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.playback.PlaybackMeta
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.ui.components.MiniPlayerBar
import com.muses.player.core.ui.components.MusesSnackbarHostContent
import com.muses.player.feature.shell.platform.PermissionsEffect
import com.muses.player.feature.shell.platform.ShellBackHandler
import com.muses.player.feature.library.AlbumDetailScreen
import com.muses.player.feature.library.AlbumsPage
import com.muses.player.feature.library.ArtistDetailScreen
import com.muses.player.feature.library.ArtistsPage
import com.muses.player.feature.library.LibraryScreen
import com.muses.player.feature.player.PlayerScreen
import com.muses.player.feature.player.QueueScreen
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.playlist.PlaylistDetailPage
import com.muses.player.feature.playlist.PlaylistsPage
import com.muses.player.feature.sources.LxScriptsScreen
import com.muses.player.feature.home.HomeScreen
import com.muses.player.feature.sources.OnlineSearchScreen
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
    /**
     * 在线曲目歌词端口（迷你条歌词模式用）。
     * 缺省空实现：未装配在线音源时一切照旧，无需判空。
     */
    private val onlineMetadataResolver: OnlineTrackMetadataResolver = NoOpOnlineTrackMetadataResolver,
    /** Muses 在线歌词匹配（AMLL → 平台五源 → LRCLIB）：脚本 `lyric` 取不到时兜底 */
    private val lyricsMatcher: LyricsMatcher? = null,
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
        OnlineTrackSession.snapshot,
    ) { songId, meta, online -> Triple(songId, meta, online) }
        .flatMapLatest { (songId, meta, online) ->
            if (songId == null) {
                flowOf(null)
            } else {
                // 修复：改为 observeById Flow，使播放时回写后迷你条与列表一致（原 one-shot 不响应 DB 更新）
                // 在线曲目不入库：同时订阅会话表快照，让「迟到登记」也能点亮迷你条标题/封面
                songDao.observeById(songId).map { song -> mergeNowPlaying(songId, song, meta, online[songId]) }
            }
        }
        .distinctUntilChanged()
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
            combine(
                playback.currentSongId,
                OnlineTrackSession.snapshot,
            ) { songId, online -> songId to online[songId] }
                .distinctUntilChanged()
                .flatMapLatest { (songId, online) ->
                    if (songId == null) {
                        flowOf(null to null)
                    } else {
                        songDao.observeById(songId).map { entity -> entity to online }
                    }
                }
                .collect { (songEntity, online) ->
                    val doc = when {
                        songEntity != null -> withContext(Dispatchers.Default) {
                            LyricsParser.parseDocument(songEntity.lyrics)
                        }
                        // 在线曲目不入库：歌词走洛雪脚本 `lyric` 动作（内存态，不写库）
                        online != null -> loadOnlineLyrics(online)
                        else -> null
                    }
                    lyricsDocument = doc
                }
        }
    }

    /**
     * 在线曲目歌词：**脚本 `lyric` 优先 → Muses 在线歌词匹配兜底**（同播放页口径）。
     *
     * 与播放页（`PlayerViewModel`）看似各自拉取，实则共享注入的 [OnlineTrackMetadataResolver]
     * （装配时为 `CachedOnlineTrackMetadataResolver` 单例），同一首曲目只真正跑一次脚本。
     */
    private suspend fun loadOnlineLyrics(song: Song): LyricsDocument? {
        val ref = OnlineTrackRef.parse(song.path) ?: return null
        val lyrics = try {
            onlineMetadataResolver.resolveLyrics(ref)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (lyrics != null) {
            // 解析器自身已带异常兜底（LxLyricParser.parse）；此处再包一层是防御未来替换实现
            val fromScript = withContext(Dispatchers.Default) {
                runCatching {
                    LxLyricParser.parse(lyrics.lyric, lyrics.tlyric, lyrics.rlyric, lyrics.lxlyric)
                }.getOrNull()
            }
            if (fromScript != null && fromScript.lines.isNotEmpty()) return fromScript
        }
        // 脚本没给歌词（多数洛雪脚本只声明 musicUrl）→ 回退 Muses 在线歌词匹配
        return lyricsMatcher?.matchDocument(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            durationSec = song.durationSec,
        )
    }

    /** 歌词进度轮询：~100ms，根据播放位置查找当前歌词行；关闭时降频到 1s */
    private fun startLyricPositionPolling() {
        viewModelScope.launch {
            while (true) {
                val enabled = miniPlayerLyricsEnabled.value
                if (enabled) {
                    val doc = lyricsDocument
                    val pos = playback.currentPosition()
                    val index = doc?.highlightedIndex(pos)
                    _currentLyricLine.value = index?.let { doc?.lines?.getOrNull(it)?.text }
                } else {
                    _currentLyricLine.value = null
                }
                delay(if (enabled) 100 else 1000)
            }
        }
    }

    private fun mergeNowPlaying(
        songId: String?,
        song: com.muses.player.core.data.db.SongEntity?,
        meta: PlaybackMeta?,
        online: Song? = null,
    ): NowPlayingUiState? {
        if (songId == null) return null
        // 在线曲目不入库：会话表条目即权威展示源（搜索结果的标题/艺术家/专辑/封面）
        if (song == null && online != null) {
            val onlineArtist = online.artist?.trim()?.takeIf { it.isNotEmpty() } ?: "未知艺术家"
            val onlineAlbum = online.album?.trim()?.takeIf { it.isNotEmpty() } ?: "未知专辑"
            return NowPlayingUiState(
                title = online.title.trim().takeIf { it.isNotEmpty() } ?: "未知歌曲",
                subtitle = "$onlineArtist - $onlineAlbum",
                artist = onlineArtist,
                coverUri = online.coverUri,
            )
        }
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MusesApp() {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        // miuix-nav 返回栈（类型化路由，存栈恢复经 kotlinx.serialization；替代 CMP Navigation）
        // 启动落地首页（首页 = 搜索框 + 排行榜 + 猜你喜欢）
        val backStack = rememberNavBackStack<MusesRoute>(MusesRoute.Home)
        // 当前栈顶（SnapshotStateList 读取即订阅，路由变化自动重组；对照原 currentBackStackEntryAsState）
        val currentKey = backStack.lastOrNull()

        // 窄屏底部导航已回到「悬浮迷你条 + 官方 FloatingNavigationBar 胶囊」：
        // 两件都是自包含组件，无需壳层共享状态（早前的推屏抽屉因需同步位移才引入 PhoneDrawerState）。
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
    // 「迷你条 ↔ 沉浸页」的可中断转场（对齐 MeloX 的 MeloXApp）：
    // 用 SeekableTransitionState + rememberTransition，然后由 Transition.AnimatedVisibility(visible = { ... })
    // 驱动两侧的进出——它能感知 seekTo 写入的中间进度，所以手势可以跟手。
    // 关键：不能用 AnimatedContent(targetState = seekableState)（内容与状态解耦后它不再做进出，会关不掉）。
    val playerTransitionState = remember { SeekableTransitionState(showPlayerOverlay) }
    val playerTransition = rememberTransition(
        transitionState = playerTransitionState,
        label = "muses-player-transition",
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(showPlayerOverlay) {
        // 外部开关（点迷你条 / 点关闭按钮 / 返回键）触发时把转场跑到目标态；
        // 手势松手后的续接由 onSettleCollapse 直接 animateTo，不走这里。
        // 必须与 PlayerShellBoundsTransform 用同一条时间线：
        // 否则封面（sharedElement 受 playerTransition 驱动）会比外壳先跑完，
        // 看上去就是「封面一上来就全尺寸」而不是随容器逐渐放大。
        // 注：刻意用 Compose 原生动画（跟随系统「动画程序时长缩放」），
        // 不用 withFrameNanos 手写帧驱动绕开它——理由见 changelog/v0.6.6。
        playerTransitionState.animateTo(
            targetState = showPlayerOverlay,
            animationSpec = tween(
                durationMillis = PlayerTransitionDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }
        var showQueueOverlay by remember { mutableStateOf(false) }

        // 全局短提示宿主状态（MusesApp 作用域持有，跨重组保持；消费见 MusesSnackbar）
        val snackbarHostState = remember { SnackbarHostState() }

        // 导航项组装（宽屏侧轨与窄屏底栏共用同一份来源）。
        // 刮削/音源收进设置页「工具」入口（胶囊图标过多会拥挤，两端同步精简）。
        // 歌单已按产品侧要求从导航移除（路由与页面保留，不再暴露入口）。
        val navItems = listOf(
            NavDestination.Home,
            NavDestination.Songs,
            NavDestination.Albums,
            NavDestination.Artists,
            NavDestination.Settings,
        ).map { dest ->
            dest.toNavItem(currentKey, backStack).let {
                // 侧轨与底栏同文案：首项统一叫「曲库」
                if (dest == NavDestination.Songs) it.copy(label = "曲库") else it
            }
        }
        // 窄屏悬浮底栏项：**只保留「探索 / 曲库 / 设置」三项**（产品侧决定；
        // 专辑/艺术家/歌单在窄屏目前没有其他入口，宽屏侧轨仍保留全部 6 项）。
        val bottomItems = listOf(
            NavDestination.Home.toNavItem(currentKey, backStack),
            NavDestination.Songs.toNavItem(currentKey, backStack).copy(label = "曲库"),
            NavDestination.Settings.toNavItem(currentKey, backStack),
        )

        val nowPlaying by viewModel.nowPlaying.collectAsState()
        val isPlaying by viewModel.isPlaying.collectAsState()
        val miniPlayerLyricsEnabled by viewModel.miniPlayerLyricsEnabled.collectAsState()
        val currentLyricLine by viewModel.currentLyricLine.collectAsState()

        // ── 滚动融合（借鉴 Halcyon 的 BottomDockMode）──
        // 列表向下滚（内容上滑）→ 紧凑；向上滚 → 展开；不消费滚动（返回 Offset.Zero）。
        // 阀值不对齐（-12 / +16）是 Halcyon 的做法：向下更敏感、向上需明确回滚，避免抖动。
        var bottomDockCompact by rememberSaveable { mutableStateOf(false) }
        val dockScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    when {
                        available.y < -12f -> bottomDockCompact = true
                        available.y > 16f -> bottomDockCompact = false
                    }
                    return Offset.Zero
                }
            }
        }
        // 无歌曲时不进入紧凑形态（没内容可融合）；当前 tab 图标用于融合胶囊左侧
        val dockCompact = bottomDockCompact && nowPlaying != null
        val currentTabItem = bottomItems.firstOrNull { it.active } ?: bottomItems.first()
        // 路由变化时重置为展开态（对齐 Halcyon 的 LaunchedEffect(currentRoute, canCompact)）：
        // 否则从列表页滚着进子页（如在线搜索/详情）时，底栏会带着上一页的融合态打开，
        // 底栏显示成三部分而非完整导航。
        LaunchedEffect(currentKey) { bottomDockCompact = false }
        // 融合过渡进度（借鉴 Halcyon）：同一个弹簧进度驱动两侧 pill 尺寸与图标缩放，
        // 让「两行 ↔ 一行」的切换不再是一帧跳变（Halcyon 用 spring(0.88, 520)）。
        val compactProgress by animateFloatAsState(
            targetValue = if (dockCompact) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f),
            label = "bottom-dock-compact",
        )

        // 底部导航槽位化：根 Scaffold bottomBar = 迷你条（上）+ 窄屏悬浮导航栏（下）。
        // 导航（aside/底栏）与内容叠层顺序由 Scaffold 统一保证，导航栏不再进 body，避免被 dock 盖住。
        // 断点与 TabsLayout 同口径（BoxWithConstraints 视口宽 ≥768dp 即平板）。
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isTabletBar = maxWidth >= TabletBreakpoint
            // 内容区保持**全屏**（不挤压）：内容可以一直滚到屏幕底、从悬浮 chrome 背后穿过，
            // 这是悬浮 Dock 该有的观感。避免遮挡靠各页面统一的 contentPadding 避让（见 TabsLayout），
            // 关键是避让值**恒定**——一旦随滚动融合变化，就会出现「避让变小后列表位置不跟着变 →
            // 展开回去时底部内容被多出的 chrome 盖住、且已到底再也滚不出来」的死角。
            val chromeSideMargin = 18.dp
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MiuixTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                // 全局短提示走官方 Snackbar 槽：定位/边距/动画全由宿主保证，绘制在底栏之上
                snackbarHost = { MusesSnackbarHostContent(snackbarHostState) },
                bottomBar = {
                    AnimatedContent(
                        targetState = dockCompact,
                        // 吃系统导航栏/手势条 inset + 8dp 保底视觉抬升（对齐 Halcyon 的 MainBottomDock）：
                        // 部分 OEM（ColorOS 某些模式）在显示手势条时会上报 nav inset = 0，
                        // 仅靠 inset 会让底栏贴住手势条；8dp 保证始终有一点呼吸。
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp),
                        transitionSpec = {
                            // 两套布局淡入淡出；内部尺寸变化由 compactProgress 弹簧连续插值
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "bottom-dock",
                    ) { compact ->
                    if (compact) {
                        // 滚动融合形态（借鉴 Halcyon）：两行合并为一行
                        // [当前 tab 图标] + [紧凑迷你播放器] + [搜索]，点左侧图标展开回两行
                        CompactPlayerDock(
                            tabIcon = currentTabItem.icon,
                            title = nowPlaying?.title ?: "暂无播放歌曲",
                            subtitle = nowPlaying?.artist ?: "未知艺术家",
                            coverUri = nowPlaying?.coverUri,
                            isPlaying = isPlaying,
                            hasSong = nowPlaying != null,
                            onExpand = { bottomDockCompact = false },
                            onOpenPlayer = { showPlayerOverlay = true },
                            onTogglePlayback = { viewModel.playPause() },
                            onOpenQueue = { showQueueOverlay = true },
                            onOpenSearch = { backStack.pushUnique(MusesRoute.OnlineSearch()) },
                            sideMargin = chromeSideMargin,
                            compactProgress = compactProgress,
                        )
                    } else {
                    // 底部槽位 = 迷你条（上）+ 窄屏悬浮导航栏（下），导航（aside/底栏）与内容叠层顺序
                    // 由 Scaffold 统一保证，导航栏不再进 body（避免被 dock 盖住）。
                    // 迷你条与底栏同为悬浮胶囊，上下叠放（形态对齐 Halcyon 的悬浮 Dock）。
                    Column(Modifier.fillMaxWidth()) {
                        BoxWithConstraints(Modifier) {
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
                    playerTransition.AnimatedVisibility(
                        visible = { !it },
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                    ) {
                        // 封面共享元素需要作用域；迷你条与沉浸页各自 provide，
                        // 两端用同一个 key，转场时 Compose 会把封面从迷你条尺寸插值到全屏。
                        CompositionLocalProvider(
                            LocalPlayerSharedTransitionScope provides this@SharedTransitionLayout,
                            LocalPlayerAnimatedVisibilityScope provides this,
                            LocalPlayerArtworkKey provides PlayerArtworkSharedKey,
                        ) {
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
                            // 与沉浸页共享同一个元素：打开/收起时由 Compose 直接把迷你条矩形
                            // 长成全屏（对齐参考实现 MeloX 的 sharedBounds）。
                            // resizeMode 保持默认（ScaleToBounds 在当前 Compose 版本不存在）。
                            // 转场丝滑度靠「减负」：已去掉动态圆角与深色遮盖（它们每帧都要
                            // 重建 clip 路径 + 全屏重绘，是卡顿的主要来源）。
                            // enter/exit 均为 None，形变完全由这条 bounds 动画驱动，不做淡入淡出。
                            .sharedBounds(
                                sharedContentState = this@SharedTransitionLayout
                                    .rememberSharedContentState(PlayerShellKey),
                                animatedVisibilityScope = this,
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                                boundsTransform = PlayerShellBoundsTransform,
                                // 早前以为这一版没有 ScaleToBounds：其实缩放绘制是函数
                            // `ResizeMode.scaleToBounds()`。但它实测不改变「内容按全屏绘制」的现象
                            // （沉浸页仍是整块涌在外面），真正让内容被限制进卡片的是 RemeasureToBounds
                            // + 去掉默认 fade + 外层 clipToBounds 这套组合。
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            )
                            .padding(horizontal = chromeSideMargin, vertical = 8.dp)
                            .fillMaxWidth(),
                    )
                    }
                    }
                    }
                    // 悬浮胶囊底栏（自研 MusesBottomDock，图标 + 文字，选中态为滑动胶囊）：
                    // 与迷你条只留迷你条自身的 8dp 下边距作间隙。曾额外 `.offset(y = 12.dp)` 下移底栏
                    // 去「收紧底部留白」，但那会让迷你条与底栏之间空出 20dp，观感松散（已移除）。
                    // 注意任何位移必须包在组件外面：经 modifier 参数传进去只会偏移内部内容，背景不动。
                    if (!isTabletBar) {
                        // 左右边距必须由**外层 Box 的 padding**提供：
                        // 若写在 MusesBottomDock 自己的 modifier 链上（fillMaxWidth().padding()），
                        // 胶囊背景仍按全宽绘制，窄屏下右侧会贴到屏幕边缘被裁（360dp 实测）。
                        // 导航胶囊 + 右侧独立搜索钮并排（对齐 Halcyon：其底部 dock 旁还有一个
                        // `BottomDockActionPill`，64×64 正方形、只有图标、与 dock 同材质）。
                        // 左右边距必须由**外层 Row 的 padding**提供：
                        // 若写在 MusesBottomDock 自己的 modifier 链上（fillMaxWidth().padding()），
                        // 胶囊背景仍按全宽绘制，窄屏下右侧会贴到屏幕边缘被裁（360dp 实测）。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MusesBottomDock(
                                modifier = Modifier.weight(1f),
                                items = bottomItems.map { item ->
                                    MusesBottomDockItem(
                                        icon = item.icon,
                                        label = item.label,
                                        active = item.active,
                                        onClick = item.onClick,
                                    )
                                },
                            )
                            MusesDockActionPill(
                                icon = TablerIcons.Search,
                                label = "搜索",
                                selected = backStack.lastOrNull() is MusesRoute.OnlineSearch,
                                onClick = { backStack.pushUnique(MusesRoute.OnlineSearch()) },
                            )
                        }
                    }
                    }
                    }
                }
            },
        ) { _ ->
            // 内容区保持全屏（不挤压）：内容滚到底时从悬浮 chrome 背后穿过，避免遮挡靠恒定避让（见 TabsLayout）。
            Box(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(dockScrollConnection),
            ) {
            // 子页面系统返回 → 上一级；主页（栈深 1）不消费，留在本页。
            // overlay 打开时不抢返回：沉浸页/队列各自消费（沉浸页见下方 ShellBackHandler，队列由 sheet 自行消费）。
            ShellBackHandler(enabled = backStack.size > 1 && !showPlayerOverlay && !showQueueOverlay) {
                backStack.pop()
            }
            // 结构恒定：overlay 打开时不得切换 TabsLayout 分支（navVisible 恒 true）——
            // 原版 Web overlay 打开仅锁主页面交互（pointer-events:none），DOM 全保留；
            // 之前按 overlayRoute 切 navVisible 会让 content（NavHost）在组合树换位销毁重建，
            // 底下列表停止绘制 → 下滑沉浸页露出纯黑（08-28 下滑露黑根因）。
            TabsLayout(
                primaryItems = navItems,
                secondaryItems = emptyList(),
                navVisible = true,
                // 列表末项避让**恒用展开态净高**（TabsLayout 默认 148dp）：
                // 曾按融合进度插值（展开 148 / 融合 72），但避让变小时列表滚动位置不会跟着变，
                // 从融合态展开回去时底部内容会被多出来的 76dp chrome 遮住（MuMu 实测：推荐卡的按钮被迷你条盖住）。
                // 恒定值的代价：融合态滞底时会多出一段空白——远好于「内容被遮看不到」。
            ) {
                AppNavHost(backStack, scrapeVm)
            }
            // 队列走 MusesBottomSheet（miuix OverlayBottomSheet）：必须组合在 Scaffold 内容层级内，
            // 根弹窗宿主（LocalRootDialogStates 由 Scaffold 提供）才能接管渲染；之前跟沉浸页一样放
            // Scaffold 外，宿主查不到导致 sheet 静默不显示（裸 Dialog 自带窗口才不受影响）。
            // 宿主层绘制在 bottomBar 之上，不会被迷你条盖住；返回/遮罩由 sheet 自行消费，不套 BackHandler。
            if (showQueueOverlay) {
                QueueScreen(onClose = { showQueueOverlay = false })
            }
            }
        }

        // 沉浸式 overlay 与 Scaffold 平级（BoxWithConstraints 双子项）：Scaffold 的 bottomBar
        // （MiniPlayerBar + 悬浮导航栏）绘制层级在 body 之上，overlay 若留在 body 内会被
        // 白色迷你条/导航栏盖住沉浸页底部控件（MuMu 实测）；平级后 overlay 必然盖过
        // bottomBar，恢复「全屏盖住不可见、不可交互」的预期层级。
        playerTransition.AnimatedVisibility(
            visible = { it },
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            ShellBackHandler { showPlayerOverlay = false }
            // 转场进行中（currentState 与 targetState 不一致）：沉浸页里最贵的模糊流光背景
            // 换成纯色，避免 sharedBounds 逐帧重排它导致掉帧。
            val playerTransitioning =
                playerTransitionState.currentState != playerTransitionState.targetState
            // 沉浸页侧同样 provide（scope 用本分支自己的 AnimatedVisibilityScope），
            // key 与迷你条一致——这是封面能「飞」过去的关键。
            CompositionLocalProvider(
                LocalPlayerSharedTransitionScope provides this@SharedTransitionLayout,
                LocalPlayerAnimatedVisibilityScope provides this,
                LocalPlayerArtworkKey provides PlayerArtworkSharedKey,
            ) {
            ScaleToBoundsBox(
                // 全屏基准：最外层 BoxWithConstraints 的约束（沉浸页 overlay 与 Scaffold 平级，同一作用域）
                fullWidth = maxWidth,
                fullHeight = maxHeight,
                modifier = Modifier
                    .fillMaxSize()
                    // 与迷你条共享同一个元素（同 key）：打开时从迷你条矩形长成全屏，收起时缩回去。
                    .sharedBounds(
                        sharedContentState = this@SharedTransitionLayout
                            .rememberSharedContentState(PlayerShellKey),
                        animatedVisibilityScope = this,
                        // 外壳的 enter/exit 必须是 None：默认是 fadeIn/fadeOut，会在整条转场上
                        // 再叠一层「整个沉浸页淡入淡出」，把 sharedBounds 的形变盖掉——
                        // 真机上看到的就是「一大块背景透明地浮在外面」而不是「圆角矩形长出来」。
                        // 形变完全交给 boundsTransform（与 animateTo 同一条 380ms 时间线）。
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                        boundsTransform = PlayerShellBoundsTransform,
                        // 与迷你条侧同口径（见该处注释：靠 RemeasureToBounds + 外层 clip 限制溢出）
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    )
                    // 形变中的外壳必须裁剪：否则内部的 fillMaxSize 背景（深色底 / 流光图）
                    // 仍按全屏绘制，转场时就会看到「沉浸页背景溢出到矩形之外」。
                    .clipToBounds(),
            ) {
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
                    isTransitioning = playerTransitioning,
                    onSeekCollapse = { fraction ->
                        // 跟手：手指每帧把进度写进转场本体（外壳 bounds 与封面共享元素都跟着它走）
                        scope.launch {
                            playerTransitionState.seekTo(
                                fraction = fraction.coerceIn(0f, 0.999f),
                                targetState = false,
                            )
                        }
                    },
                    onSettleCollapse = { collapse ->
                        // 松手：由转场本体从当前 fraction 续接动画
                        scope.launch { playerTransitionState.animateTo(targetState = !collapse) }
                        if (collapse) showPlayerOverlay = false
                    },
                )
            }
        }
        }
        }
    }
}

/**
 * 迷你条 ↔ 沉浸页的共享元素键。
 *
 * 两者用同一个 key 做 `sharedBounds`，转场时 Compose 会把起始矩形（迷你条）逐帧插值到
 * 目标矩形（全屏），从而得到「从迷你播放条上展开 / 收起」的效果，而不是普通的整页滑动。
 */
private const val PlayerShellKey = "muses-player-shell"

/**
 * 共享元素边界动画的时间线。
 *
 * 对齐参考实现 MeloX 的 `MeloXPlayerShellBoundsTransform`：360ms + FastOutSlowInEasing。
 * 注意不能用默认的 spring —— 弹簧会让父子元素各自提前/推迟到达，转场中会出现「内容已经就位
 * 但容器还在变形」的割裂感，统一 tween 才是可预期的单一时间线。
 */
private val PlayerShellBoundsTransform = BoundsTransform { _, _ ->
    // 与 [PlayerTransitionDurationMillis] 同一条时间线：外壳 bounds 与封面共享元素
    // 必须同时到点，否则会出现「内容已就位但容器还在变形」的割裂感。
    tween(durationMillis = PlayerTransitionDurationMillis, easing = FastOutSlowInEasing)
}

/**
 * 「迷你条 ↔ 沉浸页」转场时长（[PlayerShellBoundsTransform] 与 `animateTo` 同源）。
 * 380ms 略长于 MeloX 的 360ms：本项目沉浸页更重（歌词面板 + 模糊背景 + 多层覆盖），
 * 配合 FastOutSlowInEasing 前段推进快，太短会显得「一下就到」。
 */
private const val PlayerTransitionDurationMillis = 380

/**
 * 在 sharedBounds 的形变矩形里，让内容**以全屏尺寸布局后再整体缩放**填满该矩形
 * （等价于 `ResizeMode.ScaleToBounds`，但本版本要靠自己做）。
 *
 * 为什么不用 ResizeMode 自带的两种模式：
 * - `scaleToBounds()`：本版本对 sharedBounds 实测不起作用——内容仍按全屏尺寸绘制，
 *   转场时整个沉浸页背景涌到卡片外面（就是 issue #53 后续这条报障）；
 * - `RemeasureToBounds`：会真的重排内容，沉浸页缩到卡片尺寸时布局塔掉
 *   （封面吃掉全部空间、标题与控制被挤到矩形外）。
 *
 * 所以这里把「布局」与「绘制」拆开：布局固定用全屏约束（内容长什么样与全屏态完全一致），
 * 再用 `scaleX/scaleY` 把绘制结果缩放填满当前形变矩形——即椒盐音乐那种
 * 「整个播放面板连内容一起缩放」的效果，外框由外层 `clipToBounds()` 兜底。
 */
@Composable
private fun ScaleToBoundsBox(
    fullWidth: Dp,
    fullHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // fullWidth/fullHeight 必须传「全屏尺寸」（取最外层 BoxWithConstraints 的 maxWidth/maxHeight）：
    // 不能依赖 LocalWindowInfo.containerSize（实测拿不到可靠值，会让缩放退化成 1 而只剩裁剪）。
    BoxWithConstraints(modifier) {
        val scaleX = if (fullWidth > 0.dp) maxWidth / fullWidth else 1f
        val scaleY = if (fullHeight > 0.dp) maxHeight / fullHeight else 1f
        Box(
            modifier = Modifier
                .requiredSize(fullWidth, fullHeight)
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    // 左上角对齐：形变矩形与全屏内容的度量原点一致，缩放不会飘移
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            content()
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
 * 切 tab：主页单例替换 + 点当前 tab 直接回主页。
 *
 * - 目标已在栈内（含正处其子页面时点本 tab）：弹到目标之上全清，直接回到该主页；
 * - 切到别的 tab：整栈替换为新主页单例（先 add 再从头清，全程非空，miuix-nav 空栈非法）；
 * - 子页面栈不跨 tab 保留（列表由 flow 重载，滚动回到顶部），切走即丢弃，返回时逐级弹栈。
 */
private fun navigateToTab(backStack: NavBackStack, destination: NavDestination) {
    val target = destination.routeKey
    if (backStack.contains(target)) {
        while (backStack.lastOrNull() != target) backStack.removeAt(backStack.lastIndex)
        return
    }
    backStack.add(target)
    while (backStack.size > 1) backStack.removeAt(0)
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
        effects = top.yukonga.miuix.kmp.nav.core.NavDisplayEffects(
            cornerClipRadius = top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius(),
        ),
    ) {
        entry<MusesRoute.Songs> {
            // 曲库容器：内含「歌曲 / 专辑 / 艺术家」三个 Tab（窄屏底栏精简为三项后，
            // 专辑/艺术家的入口改由此提供）。U16：SongsPage 经 PlaybackPort 消费。
            val playback = org.koin.compose.koinInject<com.muses.player.core.playback.PlaybackPort>()
            // M3：刮削队列入队（ScrapeQueueStore 为 @Singleton，经 koinViewModel 载体注入）
            val scrapeQueueVm: com.muses.player.feature.scrape.ScrapeQueueAccessViewModel = koinViewModel()
            LibraryScreen(
                playback = playback,
                onEnqueueScrape = { ids -> scrapeQueueVm.enqueue(ids) },
                onAlbumClick = { albumId -> backStack.pushUnique(MusesRoute.AlbumDetail(albumId)) },
                onArtistClick = { artistId -> backStack.pushUnique(MusesRoute.ArtistDetail(artistId)) },
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
                onOpenLxScripts = { backStack.pushUnique(MusesRoute.LxScripts) },
                // 音源页进入搜索页不带关键词（旧无参语义）
                onOpenOnlineSearch = { backStack.pushUnique(MusesRoute.OnlineSearch()) },
            )
        }
        entry<MusesRoute.LxScripts>(swipeDismiss = NavSwipeDirection.LeftToRight) {
            LxScriptsScreen(onBack = { backStack.pop() })
        }
        entry<MusesRoute.Home> {
            HomeScreen(
                // 首页搜索框把关键词一并带入在线搜索页（进入即搜）
                onOpenOnlineSearch = { keyword ->
                    backStack.pushUnique(MusesRoute.OnlineSearch(keyword))
                },
                // AI 推荐配置入口：设置页「AI 推荐」分组
                onOpenAiSettings = { backStack.pushUnique(MusesRoute.Settings) },
            )
        }
        entry<MusesRoute.OnlineSearch>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
            OnlineSearchScreen(
                onBack = { backStack.pop() },
                initialKeyword = route.keyword,
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
        entry<MusesRoute.Settings> {
            SettingsScreen(
                // 窄屏底栏 5 项未含刮削/音源，经设置页「工具」区块进入；宽屏传 null 隐藏（Rail 自带）。
                onOpenSources = { backStack.pushUnique(NavDestination.Sources.routeKey) },
                onOpenScrape = { backStack.pushUnique(NavDestination.Scrape.routeKey) },
            )
        }
    }
}

/**
 * 滚动融合形态的底部胶囊（借鉴 Halcyon 的 `CompactBottomDock`）。
 *
 * 列表向下滚动时，展开态的两行（独立迷你条 + 悬浮导航栏）融合为**一行**：
 * `[当前 tab 图标] + [紧凑迷你播放器] + [搜索]`，点左侧图标展开回两行。
 *
 * 为什么保留迷你播放器而不换成更矮的紧凑版：Muses 的迷你条本身就是 64dp 悬浮胶囊，
 * 与两侧 pill 同高，融合后仍是一行完整胶囊，无需再做第二套播放器组件。
 */
@Composable
private fun CompactPlayerDock(
    tabIcon: ImageVector,
    title: String,
    subtitle: String,
    coverUri: String?,
    isPlaying: Boolean,
    hasSong: Boolean,
    onExpand: () -> Unit,
    onOpenPlayer: () -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit,
    sideMargin: Dp,
    compactProgress: Float,
) {
    val collapse = compactProgress.coerceIn(0f, 1f)
    // 借鉴 Halcyon：同一弹簧进度驱动图标缩放（连续插值，过渡不跳变）
    val iconScale = 1f - 0.14f * collapse
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sideMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左：当前 tab（点击展开回两行）
        DockIconPill(
            icon = tabIcon,
            contentDescription = "展开导航",
            onClick = onExpand,
            iconScale = iconScale,
        )
        // 中：迷你播放器（复用既有组件，weight 占满剩余宽度）
        Box(Modifier.weight(1f)) {
            MiniPlayerBar(
                title = title,
                subtitle = subtitle,
                coverUri = coverUri,
                isPlaying = isPlaying,
                hasSong = hasSong,
                coverSize = 38.dp,
                onOpenPlayer = onOpenPlayer,
                onTogglePlayback = onTogglePlayback,
                onOpenQueue = onOpenQueue,
                // 融合态宽度被两侧 pill 占去大半：关队列按钮，把宽度让给标题
                showQueueButton = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // 右：搜索
        DockIconPill(
            icon = TablerIcons.Search,
            contentDescription = "搜索",
            onClick = onOpenSearch,
            iconScale = iconScale,
        )
    }
}

/** 融合胶囊两侧图标 pill 的边长（正方形，圆角取半宽即正圆）；与迷你条同高 56dp */
private val DockPillSize = 56.dp

/**
 * 融合胶囊两侧的图标 pill。
 *
 * **材质与 [MiniPlayerBar] 严格同款**（否则三部分拼在一行会明显「不是一套」）：
 * `surfaceContainer` 底 + 官方 `dropShadow` 阴影（radius 10dp / 黑 20%）+ squircle 平滑圆角。
 * 尺寸为 **60dp 正方形**（圆角取半宽 = 正圆）：圆形必须用正方形，早期用「48×64 竖胶囊」会渲染成圆角矩形而不是圆；
 * 60dp 是在「与迷你条 64dp 尽量齐高」与「给中间标题留宽」之间的折中（高度差 4dp 肉眼基本不可辨）。
 *
 * [iconScale] 由上层按 compactProgress 插值传入，使融合/展开过程中图标尺寸连续变化。
 */
@Composable
private fun DockIconPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    iconScale: Float = 1f,
) {
    val scheme = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(DockPillSize)
            .dropShadow(
                shape = shape,
                shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.2f),
            )
            .squircleBackground(scheme.surfaceContainer, DockPillSize / 2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = scheme.onSurface,
            modifier = Modifier.size(22.dp * iconScale),
        )
    }
}
