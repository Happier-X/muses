package com.muses.player.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.feature.library.AlbumDetailScreen
import com.muses.player.feature.library.AlbumsPage
import com.muses.player.feature.library.ArtistDetailScreen
import com.muses.player.feature.library.ArtistsPage
import com.muses.player.feature.library.SongsPage
import com.muses.player.feature.player.PlayerScreen
import com.muses.player.feature.player.QueueScreen
import com.muses.player.feature.playlist.PlaylistDetailPage
import com.muses.player.feature.playlist.PlaylistsPage
import com.muses.player.feature.sources.SourcesScreen
import com.muses.player.feature.sources.WebDavBrowseScreen
import com.muses.player.feature.sources.WebDavFormScreen
import com.muses.player.R
import com.muses.player.settings.SettingsScreen
import com.muses.player.core.ui.components.MiniPlayerBar
import com.muses.player.core.ui.components.SaltEmpty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** MiniPlayerBar 的数据快照（对照 MiniPlayer.vue 的 playerState.currentSong 消费口径） */
data class NowPlayingUiState(
    val title: String,
    /** 「{artist} - {album}」，空值回退「未知艺术家/未知专辑」 */
    val subtitle: String,
    val coverUri: String?,
)

/** 主界面 ViewModel：管理权限、播放连接与 MiniPlayer 数据 */
class MainViewModel constructor(
    private val playerConnection: PlayerConnection,
    private val songDao: SongDao,
    private val songRepository: com.muses.player.core.data.repository.SongRepository,
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying

    /**
     * 当前曲信息：优先用 Room 的已回写标签（lazyScan 后），未回写前回退到 ExoPlayer 解析的 Player.mediaMetadata
     *（通知栏同源，解决「通知栏有信息但底部栏仍未知」）。
     * 关键：必须用 Player.mediaMetadata（合并动态 ID3）而非 MediaItem.mediaMetadata（静态占位）。
     * null = 无当前曲（MiniPlayer 显示空态整条，对照 `.mini-player--empty`）。
     * 修复：改为 observeById Flow，使播放时回写后迷你条与列表一致（原 one-shot getById 不响应 DB 更新）。
     */
    val nowPlaying: StateFlow<NowPlayingUiState?> = kotlinx.coroutines.flow.combine(
        playerConnection.currentMediaItem,
        playerConnection.mediaMetadata,
        playerConnection.currentMediaItem.map { it?.mediaId }.distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(null) else songDao.observeById(id)
            },
    ) { mediaItem, mediaMetadata, songEntity ->
        mediaItem?.let { item ->
            val song = songEntity
            // 合并 metadata 优先用 Player.mediaMetadata（动态解析），回退到 MediaItem.mediaMetadata（静态）
            val combined = mediaMetadata ?: item.mediaMetadata
            val metaTitle = combined.title?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val metaArtist = combined.artist?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val metaAlbum = combined.albumTitle?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val metaCover = combined.artworkUri?.toString()
            // 修复：已刮削字段（metaTitle/metaArtist/metaAlbum/metaCover 非空）优先库值，避免重刮削后仍显示旧文件标签
            val title = when {
                song == null -> metaTitle ?: "未知歌曲"
                song.metaTitle != null -> song.title
                song.tagsVersion < com.muses.player.core.media.scanner.WebDavLibraryScanner.TAGS_VERSION -> metaTitle ?: song.title
                else -> song.title
            }
            val artist = when {
                song == null -> metaArtist ?: "未知艺术家"
                song.metaArtist != null -> song.artist ?: "未知艺术家"
                song.tagsVersion < com.muses.player.core.media.scanner.WebDavLibraryScanner.TAGS_VERSION -> metaArtist ?: song.artist ?: "未知艺术家"
                else -> song.artist ?: "未知艺术家"
            }
            val album = when {
                song == null -> metaAlbum ?: "未知专辑"
                song.metaAlbum != null -> song.albumTitle ?: "未知专辑"
                song.tagsVersion < com.muses.player.core.media.scanner.WebDavLibraryScanner.TAGS_VERSION -> metaAlbum ?: song.albumTitle ?: "未知专辑"
                else -> song.albumTitle ?: "未知专辑"
            }
            val cover = when {
                song == null -> metaCover
                song.metaCover != null -> song.coverUri
                song.tagsVersion < com.muses.player.core.media.scanner.WebDavLibraryScanner.TAGS_VERSION -> metaCover ?: song.coverUri
                else -> song.coverUri
            }
            NowPlayingUiState(
                title = title,
                subtitle = "$artist - $album",
                coverUri = cover,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 存量库专辑/艺术家索引回填（幂等） */
    fun rebuildLibraryIndexes() {
        viewModelScope.launch {
            runCatching { songRepository.rebuildDerivedIndexes() }
        }
    }

    fun connectPlayer() {
        playerConnection.connect()
    }

    fun disconnectPlayer() {
        playerConnection.disconnect()
    }

    fun playPause() = playerConnection.playPause()
}

/**
 * 主框架入口 —— P1 复刻版：TabsLayout 双形态导航（aside/drawer）+ NavHost +
 * MiniPlayer 叠加。原 M1 的 ModalNavigationDrawer/Scaffold/TopAppBar 骨架已由
 * TabsPage.vue 对照实现整体替换。
 */
@Composable
fun MusesApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val viewModel: MainViewModel = koinViewModel()

    // 连接播放服务
    LaunchedEffect(Unit) {
        viewModel.connectPlayer()
        // 存量库回填：albums/artists 索引此前无维护方，启动时幂等重建一次
        viewModel.rebuildLibraryIndexes()
    }


    // 请求权限
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限结果处理（静默忽略拒绝）
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        // Android 13+ 需要 READ_MEDIA_AUDIO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        // Android 13+ 需要 POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = NavDestination.fromRoute(currentRoute) ?: NavDestination.Songs

    // 沉浸式/队列改为状态驱动的 overlay（Box 叠加于 Tabs 之上），下滑漏出背后列表而非纯黑窗口
    var showPlayerOverlay by remember { mutableStateOf(false) }
    var showQueueOverlay by remember { mutableStateOf(false) }

    // 导航项组装（map 为 inline 函数，lambda 内可直接调 Composable 取 string 资源）
    val primaryItems = NavDestination.Primary.map { dest -> dest.toNavItem(currentRoute, navController) }
    val secondaryItems = NavDestination.Secondary.map { dest -> dest.toNavItem(currentRoute, navController) }

    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Box(Modifier.fillMaxSize()) {
        // 结构恒定：overlay 打开时不得切换 TabsLayout 分支（navVisible 恒 true）——
        // 原版 Web overlay 打开仅锁主页面交互（pointer-events:none），DOM 全保留；
        // 之前按 overlayRoute 切 navVisible 会让 content（NavHost）在组合树换位销毁重建，
        // 底下列表停止绘制 → 下滑沉浸页露出纯黑（08-28 下滑露黑根因）。
        // overlay 全屏在上层，主页面的 navbar/MiniPlayer 被盖住不可见、不可交互。
        TabsLayout(
            primaryItems = primaryItems,
            secondaryItems = secondaryItems,
            navVisible = true,
            bottomBar = {
                Box(Modifier.navigationBarsPadding()) {
                    MiniPlayerBar(
                        title = nowPlaying?.title ?: stringResource(R.string.mini_empty_title),
                        subtitle = nowPlaying?.subtitle ?: stringResource(
                            R.string.mini_unknown_artist,
                        ) + " - " + stringResource(R.string.mini_unknown_album),
                        coverUri = nowPlaying?.coverUri,
                        isPlaying = isPlaying,
                        hasSong = nowPlaying != null,
                        onOpenPlayer = { showPlayerOverlay = true },
                        onTogglePlayback = { viewModel.playPause() },
                        onOpenQueue = { showQueueOverlay = true },
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                    )
                }
            },
        ) {
            AppNavHost(navController)
        }
        if (showPlayerOverlay) {
            BackHandler { showPlayerOverlay = false }
            Box(Modifier.fillMaxSize()) {
                val playerVm: com.muses.player.feature.player.PlayerViewModel = koinViewModel()
                val currentMediaItem by playerVm.currentMediaItem.collectAsState()
                var showEditMeta by remember { mutableStateOf(false) }
                if (showEditMeta) {
                    val editSong = currentMediaItem?.let { item ->
                        com.muses.player.core.model.Song(
                            id = item.mediaId,
                            sourceId = "",
                            path = item.localConfiguration?.uri?.toString() ?: "",
                            title = item.mediaMetadata.title?.toString() ?: "未知歌曲",
                            artist = item.mediaMetadata.artist?.toString(),
                            album = item.mediaMetadata.albumTitle?.toString(),
                            coverUri = item.mediaMetadata.artworkUri?.toString(),
                        )
                    }
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
            BackHandler { showQueueOverlay = false }
            Box(Modifier.fillMaxSize()) {
                QueueScreen(onClose = { showQueueOverlay = false })
            }
        }
    }
}

/** 导航项组装（图标/文案/激活判定均来自 NavDestination 的 Web 层映射） */
@Composable
private fun NavDestination.toNavItem(
    currentRoute: String?,
    navController: NavHostController,
): SaltNavItem = SaltNavItem(
    icon = icon,
    label = stringResource(labelRes),
    active = isActive(currentRoute),
    onClick = { navigateTo(navController, this) },
)

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Songs.route,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(NavDestination.Songs.route) {
            val playerConnection = koinViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
            // M3：刮削队列入队（ScrapeQueueStore 为 @Singleton，经 koinViewModel 载体注入）
            val scrapeVm: com.muses.player.feature.scrape.ScrapeQueueAccessViewModel = koinViewModel()
            SongsPage(
                playerConnection = playerConnection,
                onEnqueueScrape = { ids -> scrapeVm.enqueue(ids) },
            )
        }
        composable(NavDestination.Albums.route) {
            AlbumsPage(
                onAlbumClick = { albumId ->
                    navController.navigate(DetailRoutes.albumDetail(albumId))
                },
            )
        }
        composable(NavDestination.Artists.route) {
            ArtistsPage(
                onArtistClick = { artistId ->
                    navController.navigate(DetailRoutes.artistDetail(artistId))
                },
            )
        }
        composable(DetailRoutes.ALBUM_DETAIL) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            val playerConnection = koinViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
            AlbumDetailScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                playerConnection = playerConnection,
            )
        }
        composable(DetailRoutes.ARTIST_DETAIL) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            val playerConnection = koinViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
            ArtistDetailScreen(
                artistId = artistId,
                onBack = { navController.popBackStack() },
                playerConnection = playerConnection,
            )
        }
        composable(NavDestination.Playlists.route) {
            PlaylistsPage(onOpenPlaylist = { playlistId ->
                navController.navigate("playlist/$playlistId") { launchSingleTop = true }
            })
        }
        composable(route = "playlist/{playlistId}") { backStackEntry ->
            PlaylistDetailPage(
                playlistId = checkNotNull(backStackEntry.arguments?.getString("playlistId")),
                onBack = { navController.popBackStack() },
            )
        }
        // 刮削页随 M3 复刻，当前为占位空态
        composable(NavDestination.Scrape.route) {
            com.muses.player.feature.scrape.ScrapeScreen(
                onOpenReview = { songId ->
                    navController.navigate(DetailRoutes.scrapeReview(songId)) { launchSingleTop = true }
                },
                // S3 逐首审核：带 queue 上下文进入审核页（「应用并下一首」推进用）
                onStartReviewQueue = { firstSongId, queue ->
                    navController.navigate(DetailRoutes.scrapeReview(firstSongId, queue)) { launchSingleTop = true }
                },
            )
        }
        // 单曲刮削审核页（Tagger 式就地审核，design §2.1）
        composable(
            route = DetailRoutes.SCRAPE_REVIEW,
            arguments = listOf(
                androidx.navigation.navArgument("songId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("queue") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            // S3：待审队列宿主在 Scrape 页的 VM 实例（跨 destination 共享，需按回退栈取同实例，
            // 不可直接 koinViewModel()——那是审核页自己的 scope）
            val scrapeEntry = remember(it) {
                try {
                    navController.getBackStackEntry(NavDestination.Scrape.route)
                } catch (_: Exception) {
                    null
                }
            }
            val scrapeVm: com.muses.player.feature.scrape.ScrapeViewModel? = scrapeEntry?.let { entry ->
                koinViewModel(viewModelStoreOwner = entry)
            }
            com.muses.player.feature.scrape.ScrapeReviewScreen(
                onBack = { navController.popBackStack() },
                // S3「应用并下一首」：同步预览（剔除已写回者）→ 推进待审队列 → 打开下一首（结束则返回）
                onAppliedAndNext = { writtenSongId, _ ->
                    scrapeVm?.refreshAfterExternalWriteback(writtenSongId)
                    val next = scrapeVm?.advanceReview(writtenSongId)
                    if (next != null) {
                        val rest = scrapeVm?.pendingReviewQueue?.value.orEmpty()
                        navController.navigate(DetailRoutes.scrapeReview(next, rest)) {
                            popUpTo(DetailRoutes.SCRAPE_REVIEW) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                // S3 手动返回：清待审队列，不强推下一首
                onManualBack = { scrapeVm?.cancelReviewQueue() },
            )
        }
        composable(NavDestination.Sources.route) { SourcesScreen(
            onOpenWebdavAdd = { navController.navigate(DetailRoutes.SOURCE_WEBDAV_ADD) },
            onOpenWebdavEdit = { sourceId ->
                navController.navigate(DetailRoutes.sourceWebdavEdit(sourceId))
            },
        ) }
        // 注意顺序：browse 是固定段，必须在 {sourceId} 参数路由之前声明，
        // 否则会被参数匹配吞掉（Navigation Compose 按声明顺序匹配）
        composable(
            route = "${DetailRoutes.SOURCE_WEBDAV_BROWSE}?mode={mode}" +
                "&initialPath={initialPath}&serverUrl={serverUrl}" +
                "&username={username}&password={password}",
            arguments = listOf(
                androidx.navigation.navArgument("mode") { defaultValue = "multiple" },
                androidx.navigation.navArgument("initialPath") { defaultValue = "/" },
                androidx.navigation.navArgument("serverUrl") { defaultValue = "" },
                androidx.navigation.navArgument("username") { defaultValue = "" },
                androidx.navigation.navArgument("password") { defaultValue = "" },
            ),
        ) { backStackEntry ->
            val args = checkNotNull(backStackEntry.arguments)
            WebDavBrowseScreen(
                mode = args.getString("mode") ?: "multiple",
                initialPath = args.getString("initialPath") ?: "/",
                serverUrl = args.getString("serverUrl") ?: "",
                username = args.getString("username") ?: "",
                password = args.getString("password") ?: "",
                onBack = { navController.popBackStack() },
                onConfirm = { paths ->
                    // 结果已由浏览页写入 WebDavBrowseResultHolder，这里只回退
                    navController.popBackStack()
                },
            )
        }
        composable(DetailRoutes.SOURCE_WEBDAV_ADD) {
            WebDavFormScreen(
                sourceId = null,
                onBack = { navController.popBackStack() },
                onBrowse = { mode, initialPath, serverUrl, username, password ->
                    navigateToWebdavBrowse(
                        navController, mode, initialPath, serverUrl, username, password,
                    )
                },
            )
        }
        composable(DetailRoutes.SOURCE_WEBDAV_EDIT) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId")
            WebDavFormScreen(
                sourceId = sourceId,
                onBack = { navController.popBackStack() },
                onBrowse = { mode, initialPath, serverUrl, username, password ->
                    navigateToWebdavBrowse(
                        navController, mode, initialPath, serverUrl, username, password,
                    )
                },
            )
        }
        composable(NavDestination.Settings.route) { SettingsScreen() }
        composable(NavDestination.NowPlaying.route) {
            // M3：编辑歌曲信息弹窗宿主（当前曲经 PlayerViewModel 反查）
            val playerVm: com.muses.player.feature.player.PlayerViewModel = koinViewModel()
            val currentMediaItem by playerVm.currentMediaItem.collectAsState()
            var showEditMeta by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            if (showEditMeta) {
                val editSong = currentMediaItem?.let { item ->
                    com.muses.player.core.model.Song(
                        id = item.mediaId,
                        sourceId = "",
                        path = item.localConfiguration?.uri?.toString() ?: "",
                        title = item.mediaMetadata.title?.toString() ?: "未知歌曲",
                        artist = item.mediaMetadata.artist?.toString(),
                        album = item.mediaMetadata.albumTitle?.toString(),
                        coverUri = item.mediaMetadata.artworkUri?.toString(),
                    )
                }
                com.muses.player.feature.scrape.EditMetaSheet(
                    song = editSong,
                    onDismiss = { showEditMeta = false },
                )
            }
            PlayerScreen(
                onClose = { navController.popBackStack() },
                onOpenQueue = { navController.navigate(NavDestination.Queue.route) },
                onOpenEditMeta = { showEditMeta = true },
            )
        }
        composable(NavDestination.Queue.route) {
            QueueScreen(onClose = { navController.popBackStack() })
        }
    }
}

/** P5 前的占位页（刮削）：Salt 空态观感 */
@Composable
private fun PlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SaltEmpty(
            title = stringResource(R.string.placeholder_page_title),
            description = stringResource(R.string.placeholder_page_description),
        )
    }
}

/** 跳转目录浏览页：连接信息经 URL query 传参（含密码，不落日志） */
private fun navigateToWebdavBrowse(
    navController: NavHostController,
    mode: String,
    initialPath: String,
    serverUrl: String,
    username: String,
    password: String,
) {
    val encoded = { value: String ->
        java.net.URLEncoder.encode(value, "UTF-8")
    }
    navController.navigate(
        "${DetailRoutes.SOURCE_WEBDAV_BROWSE}?mode=$mode" +
            "&initialPath=${encoded(initialPath)}" +
            "&serverUrl=${encoded(serverUrl)}" +
            "&username=${encoded(username)}" +
            "&password=${encoded(password)}",
    )
}

private fun navigateTo(navController: NavHostController, destination: NavDestination) {
    if (destination == NavDestination.Songs) {
        navController.popBackStack(NavDestination.Songs.route, inclusive = false)
        return
    }
    navController.navigate(destination.route) {
        popUpTo(NavDestination.Songs.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
