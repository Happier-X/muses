package com.muses.player.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    val state = remember {
        WindowState(
            position = WindowPosition(100.dp, 100.dp),
            size = DpSize(1280.dp, 800.dp),
        )
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
        )
    }
}
