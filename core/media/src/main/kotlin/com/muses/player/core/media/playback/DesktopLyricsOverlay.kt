package com.muses.player.core.media.playback

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.muses.player.core.lyrics.DesktopLyricsText
import com.muses.player.core.ui.components.DesktopLyricsCard
import com.muses.player.core.ui.theme.MusesTheme

/** 由播放服务持有，不获取焦点；关闭或服务停止时立即移除窗口。 */
class DesktopLyricsOverlay(private val context: Context, private val onClose: () -> Unit) {
    private val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val preferences = context.getSharedPreferences("desktop_lyrics_window", Context.MODE_PRIVATE)
    private val text = mutableStateOf(DesktopLyricsText("Muses", null))
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var params: WindowManager.LayoutParams? = null

    fun update(value: DesktopLyricsText) {
        if (!Settings.canDrawOverlays(context)) { hide(); return }
        text.value = value
        val metrics = context.resources.displayMetrics
        val width = (metrics.widthPixels - (24 * metrics.density).toInt()).coerceAtMost((520 * metrics.density).toInt())
        // 给 Compose 的主题浮层明确高度，避免根布局按整屏测量后把窗口位置夹到顶部。
        val windowHeight = (76 * metrics.scaledDensity + 12 * metrics.density).toInt()
        view?.let { current ->
            params?.let { layout ->
                val maxX = (metrics.widthPixels - width).coerceAtLeast(0)
                val maxY = (metrics.heightPixels - windowHeight).coerceAtLeast(0)
                if (layout.width != width || layout.height != windowHeight || layout.x > maxX || layout.y > maxY) {
                    layout.width = width
                    layout.height = windowHeight
                    layout.x = layout.x.coerceIn(0, maxX)
                    layout.y = layout.y.coerceIn(0, maxY)
                    manager.updateViewLayout(current, layout)
                }
            }
            return
        }
        val layout = WindowManager.LayoutParams(width, windowHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = preferences.getInt("x", (12 * metrics.density).toInt()).coerceIn(0, (metrics.widthPixels - width).coerceAtLeast(0))
            y = preferences.getInt("y", (metrics.heightPixels * 0.18f).toInt()).coerceIn(0, (metrics.heightPixels - windowHeight).coerceAtLeast(0))
        }
        val lifecycleOwner = OverlayOwner()
        val content = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MusesTheme {
                    DesktopLyricsCard(text.value.primary, text.value.secondary, onClose,
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(onDragEnd = {
                                preferences.edit().putInt("x", layout.x).putInt("y", layout.y).apply()
                            }) { change, delta ->
                                change.consume()
                                val display = context.resources.displayMetrics
                                layout.x = (layout.x + delta.x.toInt()).coerceIn(0, (display.widthPixels - layout.width).coerceAtLeast(0))
                                layout.y = (layout.y + delta.y.toInt()).coerceIn(0, (display.heightPixels - height).coerceAtLeast(0))
                                runCatching { manager.updateViewLayout(this@apply, layout) }
                            }
                        }, words = text.value.words, positionMs = text.value.positionMs, isPlaying = text.value.isPlaying)
                }
            }
        }
        try {
            manager.addView(content, layout)
            view = content
            owner = lifecycleOwner
            params = layout
        } catch (e: Exception) {
            content.disposeComposition()
            lifecycleOwner.destroy()
            throw e
        }
    }

    fun hide() {
        view?.let { runCatching { manager.removeViewImmediate(it) }; it.disposeComposition() }
        owner?.destroy()
        view = null
        owner = null
        params = null
    }

    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry
        init {
            saved.performAttach()
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
