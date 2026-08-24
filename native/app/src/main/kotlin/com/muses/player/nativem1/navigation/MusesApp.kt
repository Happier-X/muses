package com.muses.player.nativem1.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.muses.player.nativem1.theme.GlassLevel
import com.muses.player.nativem1.theme.GlassSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 主界面 ViewModel：管理权限和播放连接 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val currentMediaItem: StateFlow<MediaItem?> = playerConnection.currentMediaItem
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying

    fun connectPlayer() {
        playerConnection.connect()
    }

    fun disconnectPlayer() {
        playerConnection.disconnect()
    }

    fun playPause() = playerConnection.playPause()

    fun skipToNext() = playerConnection.skipToNext()
}

private fun NavDestination.icon(): ImageVector = when (this) {
    NavDestination.Songs -> Icons.Filled.MusicNote
    NavDestination.Albums -> Icons.Filled.PlayArrow
    NavDestination.Artists -> Icons.Filled.Person
    NavDestination.Sources -> Icons.Filled.Settings
    NavDestination.Playlists -> Icons.Filled.QueueMusic
    NavDestination.NowPlaying -> Icons.Filled.PlayArrow
    NavDestination.Queue -> Icons.Filled.QueueMusic
}

/** 主界面骨架：抽屉导航 + NavHost + MiniPlayer */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusesApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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

    // 收集当前播放歌曲
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                current = current,
                onNavigate = { destination ->
                    navigateTo(navController, destination)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(current.labelRes)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (current != NavDestination.NowPlaying && current != NavDestination.Queue) {
                    MiniPlayer(
                        currentMediaItem = currentMediaItem,
                        isPlaying = isPlaying,
                        onOpenPlayer = { navigateTo(navController, NavDestination.NowPlaying) },
                        onPlayPause = { viewModel.playPause() },
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Songs.route,
        modifier = modifier,
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
        composable(NavDestination.Sources.route) { SourcesScreen() }
        composable(NavDestination.NowPlaying.route) { PlayerScreen(
            onOpenQueue = { navController.navigate(NavDestination.Queue.route) },
        ) }
        composable(NavDestination.Queue.route) { QueueScreen() }
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

@Composable
private fun DrawerContent(
    current: NavDestination,
    onNavigate: (NavDestination) -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Spacer(Modifier.padding(top = 24.dp))
        Column(Modifier.padding(horizontal = 12.dp)) {
            NavDestination.entries.filter { it != NavDestination.Queue }.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.labelRes)) },
                    icon = { Icon(destination.icon(), contentDescription = null) },
                    selected = destination == current,
                    onClick = { onNavigate(destination) },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

/** MiniPlayer：显示当前播放歌曲信息和控制按钮 */
@Composable
private fun MiniPlayer(
    currentMediaItem: MediaItem?,
    isPlaying: Boolean,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        level = GlassLevel.Medium,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未在播放",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (currentMediaItem != null) {
                    Text(
                        text = currentMediaItem.mediaMetadata.artist?.toString() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                )
            }
            IconButton(onClick = onOpenPlayer) {
                Icon(Icons.Filled.MusicNote, contentDescription = stringResource(R.string.cd_open_player))
            }
        }
    }
}
