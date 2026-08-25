package com.muses.player.nativem1.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.feature.library.AlbumDetailScreen
import com.muses.player.feature.library.AlbumsScreen
import com.muses.player.feature.library.ArtistDetailScreen
import com.muses.player.feature.library.ArtistsScreen
import com.muses.player.feature.library.SongsScreen
import com.muses.player.feature.player.PlayerScreen
import com.muses.player.feature.player.QueueScreen
import com.muses.player.feature.playlist.PlaylistDetailPage
import com.muses.player.feature.playlist.PlaylistsPage
import com.muses.player.feature.sources.SourcesScreen
import com.muses.player.nativem1.R
import com.muses.player.nativem1.onboarding.OnboardingScreen
import com.muses.player.nativem1.ui.MiniPlayerBar
import com.muses.player.nativem1.ui.SaltEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** MiniPlayerBar 的数据快照（对照 MiniPlayer.vue 的 playerState.currentSong 消费口径） */
data class NowPlayingUiState(
    val title: String,
    /** 「{artist} - {album}」，空值回退「未知艺术家/未知专辑」 */
    val subtitle: String,
    val coverUri: String?,
)

/** 主界面 ViewModel：管理权限、播放连接与 MiniPlayer 数据 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val songDao: SongDao,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying

    /**
     * 当前曲信息：currentMediaItem.mediaId（= song.id）反查 Room。
     * null = 无当前曲（MiniPlayer 显示空态整条，对照 `.mini-player--empty`）。
     */
    val nowPlaying: StateFlow<NowPlayingUiState?> = playerConnection.currentMediaItem
        .map { it?.mediaId }
        .distinctUntilChanged()
        .map { songId ->
            songId?.let { runCatching { songDao.getById(it) }.getOrNull() }?.let { song ->
                NowPlayingUiState(
                    title = song.title,
                    subtitle = "${song.artist ?: "未知艺术家"} - ${song.albumTitle ?: "未知专辑"}",
                    coverUri = song.coverUri,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    val viewModel: MainViewModel = hiltViewModel()

    // 检查首次启动
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    // 连接播放服务
    LaunchedEffect(Unit) {
        viewModel.connectPlayer()
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

    // 首次启动引导
    if (isFirstLaunch) {
        OnboardingScreen(
            onComplete = {
                // 引导完成后重新检查状态
            },
        )
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = NavDestination.fromRoute(currentRoute) ?: NavDestination.Songs

    // 播放页/队列页为覆盖路由形态：无导航 chrome（Web 层 popup 盖住 tabs-layout）
    val overlayRoute = current == NavDestination.NowPlaying || current == NavDestination.Queue

    // 导航项组装（map 为 inline 函数，lambda 内可直接调 Composable 取 string 资源）
    val primaryItems = NavDestination.Primary.map { dest -> dest.toNavItem(currentRoute, navController) }
    val secondaryItems = NavDestination.Secondary.map { dest -> dest.toNavItem(currentRoute, navController) }

    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    TabsLayout(
        primaryItems = primaryItems,
        secondaryItems = secondaryItems,
        navVisible = !overlayRoute,
        bottomBar = {
            // MiniPlayer：覆盖路由上隐藏（对照 Web 层 popup 盖过 mini-player z 序）
            if (!overlayRoute) {
                Box(Modifier.navigationBarsPadding()) {
                    MiniPlayerBar(
                        title = nowPlaying?.title ?: stringResource(R.string.mini_empty_title),
                        subtitle = nowPlaying?.subtitle ?: stringResource(
                            R.string.mini_unknown_artist,
                        ) + " - " + stringResource(R.string.mini_unknown_album),
                        coverUri = nowPlaying?.coverUri,
                        isPlaying = isPlaying,
                        hasSong = nowPlaying != null,
                        onOpenPlayer = { navigateTo(navController, NavDestination.NowPlaying) },
                        onTogglePlayback = { viewModel.playPause() },
                        onOpenQueue = { navigateTo(navController, NavDestination.Queue) },
                        modifier = Modifier
                            // .mini-player 定位：left/right 18px / bottom safe-bottom+8px
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        AppNavHost(navController)
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
            val viewModel: com.muses.player.feature.library.SongsViewModel = hiltViewModel()
            val playerConnection = androidx.hilt.navigation.compose.hiltViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
            SongsScreen(
                viewModel = viewModel,
                playerConnection = playerConnection,
            )
        }
        composable(NavDestination.Albums.route) {
            AlbumsScreen(
                onAlbumClick = { albumId ->
                    navController.navigate(DetailRoutes.albumDetail(albumId))
                },
            )
        }
        composable(NavDestination.Artists.route) {
            ArtistsScreen(
                onArtistClick = { artistId ->
                    navController.navigate(DetailRoutes.artistDetail(artistId))
                },
            )
        }
        composable(DetailRoutes.ALBUM_DETAIL) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            val playerConnection = androidx.hilt.navigation.compose.hiltViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
            AlbumDetailScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                playerConnection = playerConnection,
            )
        }
        composable(DetailRoutes.ARTIST_DETAIL) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            val playerConnection = androidx.hilt.navigation.compose.hiltViewModel<com.muses.player.feature.player.PlayerViewModel>().playerConnection
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
        // 刮削/设置页随 P5 批次复刻，当前为占位空态
        composable(NavDestination.Scrape.route) { PlaceholderScreen() }
        composable(NavDestination.Sources.route) { SourcesScreen() }
        composable(NavDestination.Settings.route) { PlaceholderScreen() }
        composable(NavDestination.NowPlaying.route) { PlayerScreen(
            onOpenQueue = { navController.navigate(NavDestination.Queue.route) },
        ) }
        composable(NavDestination.Queue.route) { QueueScreen() }
    }
}

/** P5 前的占位页（刮削/设置）：Salt 空态观感 */
@Composable
private fun PlaceholderScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SaltEmpty(
            title = stringResource(R.string.placeholder_page_title),
            description = stringResource(R.string.placeholder_page_description),
        )
    }
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
