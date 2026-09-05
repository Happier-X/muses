package com.muses.player.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.desktop.tray.DesktopTray

fun main() = application {
    val state = remember {
        WindowState(
            position = WindowPosition(100.dp, 100.dp),
            size = DpSize(1280.dp, 800.dp),
        )
    }
    // 单一 hook：screens 与托盘共享同一播放状态源（DesktopContainer.playerPort 非单例）
    val playerHook = remember { DesktopPlayerHook() }
    val tray = remember {
        DesktopTray(
            isPlaying = playerHook.isPlaying,
            onShowMainWindow = { state.isMinimized = false },
            onTogglePlay = playerHook::togglePlayPause,
            onNext = playerHook::next,
            onPrevious = playerHook::previous,
            onExit = ::exitApplication,
        )
    }
    DisposableEffect(Unit) {
        tray.install()
        onDispose { tray.uninstall() }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Muses",
        undecorated = true,
        transparent = false,
    ) {
        MusesDesktopApp(
            windowState = state,
            onClose = ::exitApplication,
            playerHook = playerHook,
        )
    }
}
