package com.muses.player.desktop

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.lyrics.DesktopLyricsState
import com.muses.player.core.lyrics.desktopLyricsText
import com.muses.player.core.model.lyrics.CoverAccentColorProvider
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.ui.components.DesktopLyricsCard
import com.muses.player.core.ui.theme.MusesTheme
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 桌面悬浮歌词：直接拖动，单击展开播放与样式控制面板。
 * 锁定后整窗穿透，从应用设置解除锁定。
 * - 颜色跟随封面强调色开关，关闭时回落默认歌词配色。
 */
@Composable
fun DesktopLyricsWindow() {
    val settings = koinInject<SettingsRepository>()
    val enabled by settings.desktopLyricsEnabled.collectAsState(false)
    val translation by settings.lyricTranslationEnabled.collectAsState(true)
    val coverAccentEnabled by settings.coverContentColorEnabled.collectAsState(true)
    val songs = koinInject<SongRepository>()
    val colors = koinInject<CoverAccentColorProvider>()
    val snapshot by DesktopLyricsState.snapshot.collectAsState()
    val playback = DesktopRuntime.playerHook()
    val songId by playback.currentSongId.collectAsState()
    val position by playback.positionMs.collectAsState()
    val playing by playback.isPlaying.collectAsState()
    val artwork by playback.artworkUri.collectAsState()
    val scope = rememberCoroutineScope()
    val windowState = rememberWindowState(width = 600.dp, height = Dp.Unspecified, position = WindowPosition(Alignment.BottomCenter))
    val locked by settings.desktopLyricsLocked.collectAsState(false)
    val fontSize by settings.desktopLyricsFontSize.collectAsState(22L)
    val selectedColor by settings.desktopLyricsColor.collectAsState(0L)
    var controlsVisible by remember { mutableStateOf(false) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val showing = enabled && songId != null && snapshot.songId == songId
    LaunchedEffect(showing, locked) { if (!showing || locked) { controlsVisible = false; hideJob?.cancel() } }
    LaunchedEffect(showing) { DesktopLyricsInteraction.setVisible(showing) }
    DisposableEffect(Unit) { onDispose { DesktopLyricsInteraction.setVisible(false) } }
    val accent by produceState<Color?>(null, songId, artwork, coverAccentEnabled, enabled) {
        value = null
        if (enabled && coverAccentEnabled && songId != null) {
            while (true) {
                value = try {
                    val song = songs.getSong(songId!!) ?: OnlineTrackSession.find(songId!!)
                    val uri = song?.coverUri?.takeIf { it.isNotBlank() } ?: artwork?.takeIf { song?.metaSources?.cover == null }
                    uri?.let { colors.colorFor(it)?.let(::Color) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
                delay(2000)
            }
        }
    }
    if (showing) {
        val text = desktopLyricsText(snapshot, position, translation, playing)
        val close = { scope.launch { settings.setDesktopLyricsEnabled(false) }; Unit }
        fun showControls() {
            controlsVisible = !controlsVisible
            hideJob?.cancel()
            if (controlsVisible) hideJob = scope.launch { delay(5000); controlsVisible = false }
        }
        fun interact(action: () -> Unit) {
            hideJob?.cancel()
            action()
            hideJob = scope.launch { delay(5000); controlsVisible = false }
        }
        Window(
            onCloseRequest = close,
            state = windowState,
            title = "Muses 桌面歌词",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = false,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(window, locked) {
                if (Platform.isWindows()) {
                    while (!window.isDisplayable) delay(16)
                    val hwnd = HWND(Native.getWindowPointer(window))
                    val style = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
                    User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE,
                        if (locked) style or WinUser.WS_EX_LAYERED or WinUser.WS_EX_TRANSPARENT
                        else style and WinUser.WS_EX_TRANSPARENT.inv())
                }
            }
            fun move(dx: Float, dy: Float) {
                windowState.position = with(density) {
                    WindowPosition.Absolute((window.x + dx.toInt()).toDp(), (window.y + dy.toInt()).toDp())
                }
            }
            MusesTheme {
                DesktopLyricsCard(text.primary, text.secondary, close,
                    modifier = Modifier.pointerInput(locked) {
                        if (!locked) detectDragGestures(onDragStart = { hideJob?.cancel() },
                            onDragEnd = { if (controlsVisible) interact {} },
                            onDragCancel = {}) { change, delta -> change.consume(); move(delta.x, delta.y) }
                    },
                    words = text.words, positionMs = text.positionMs, isPlaying = text.isPlaying,
                    accentColor = accent, controlsVisible = controlsVisible,
                    onTap = { showControls() },
                    onLock = { controlsVisible = false; scope.launch { settings.setDesktopLyricsLocked(true) } },
                    title = snapshot.title, fontSize = fontSize.toInt(), selectedColor = selectedColor.takeIf { it != 0L }?.let { Color(it.toInt()) },
                    onPrevious = { interact { playback.previous() } },
                    onPlayPause = { interact { playback.togglePlayPause() } },
                    onNext = { interact { playback.next() } },
                    onColor = { color -> interact { scope.launch { settings.setDesktopLyricsColor(color) } } },
                    onFontSize = { size -> interact { scope.launch { settings.setDesktopLyricsFontSize(size.toLong()) } } })
            }
        }
    }
}
