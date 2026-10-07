package com.muses.player.desktop

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.lyrics.DesktopLyricsState
import com.muses.player.core.lyrics.desktopLyricsText
import com.muses.player.core.ui.components.DesktopLyricsCard
import com.muses.player.core.ui.theme.MusesTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DesktopLyricsWindow() {
    val settings = koinInject<SettingsRepository>()
    val enabled by settings.desktopLyricsEnabled.collectAsState(false)
    val translation by settings.lyricTranslationEnabled.collectAsState(true)
    val snapshot by DesktopLyricsState.snapshot.collectAsState()
    val playback = DesktopRuntime.playerHook()
    val songId by playback.currentSongId.collectAsState()
    val position by playback.positionMs.collectAsState()
    val playing by playback.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()
    val windowState = rememberWindowState(width = 600.dp, height = Dp.Unspecified, position = WindowPosition(Alignment.BottomCenter))
    if (enabled && songId != null && snapshot.songId == songId) {
        val text = desktopLyricsText(snapshot, position, translation, playing)
        val close = { scope.launch { settings.setDesktopLyricsEnabled(false) }; Unit }
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
            MusesTheme {
                WindowDraggableArea {
                    DesktopLyricsCard(text.primary, text.secondary, close, words = text.words,
                        positionMs = text.positionMs, isPlaying = text.isPlaying)
                }
            }
        }
    }
}
