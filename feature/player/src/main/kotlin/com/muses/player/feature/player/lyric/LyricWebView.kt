package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/**
 * 基于 WebView 的 AMLL 网页版歌词渲染 — 1:1 复刻 AMLL-DroidMate AMLLLyricsView.kt
 *
 * 对齐点（vs 之前 file:// 与半抄版本）：
 * 1. WebViewAssetLoader https://appassets.androidplatform.net/assets/amll/ 虚拟域名，规避 file:// CORS
 * 2. LAYER_TYPE_HARDWARE + TRANSPARENT 透出底层 FlowingLightBackdrop
 * 3. isPageReady 闸门 + lastLyrics/lastIsPlaying/时间节流 去重，避免 recompose 频刷 JS
 * 4. 乐观 seek：onLineClick 立即 post updateTime 到 JS，防旧时间回滚
 * 5. onRelease 完整销毁防泄漏
 * 6. 兼容补丁：为 Chromium 110 注入 mix-blend 降级样式，避免 plus-lighter + 无背景时全透明
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LyricWebView(
    modifier: Modifier = Modifier,
    lyrics: SyncedLyrics?,
    positionMs: () -> Int,
    isPlaying: () -> Boolean,
    showTranslation: Boolean,
    fontSizeSp: Float,
    onSeek: (Long) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
) {
    val context = LocalContext.current
    var isPageReady by remember { mutableStateOf(false) }
    var lastLyrics by remember { mutableStateOf<SyncedLyrics?>(null) }
    var lastIsPlaying by remember { mutableStateOf<Boolean?>(null) }
    var lastTimeUpdate by remember { mutableLongStateOf(0L) }
    var lastFontSize by remember { mutableStateOf<Float?>(null) }

    val onSeekState = rememberUpdatedState(onSeek)
    val isPlayingState = rememberUpdatedState(isPlaying)
    val positionMsState = rememberUpdatedState(positionMs)

    // 节流：60Hz 16ms，兼顾逐词平滑与 UI 线程
    val frameIntervalMs = 32L

    val lyricJson = remember(lyrics, showTranslation) {
        lyrics?.toLyricLinesJson(showTranslation)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView.setWebContentsDebuggingEnabled(true)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.TRANSPARENT)
                // 硬件加速：Chromium 合成器缓存静态帧，增量合成，避免每帧全量重绘
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        if (isPageReady) isPageReady = false
                        if (lastLyrics != null) lastLyrics = null
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        // 兼容补丁：Chromium 110 的 plus-lighter + 无背景 时文字全透明，强制降级为 normal
                        // 同时确保 --amll-lp-color 有值，否则 mask 渐变依赖的根变量为空
                        view.evaluateJavascript(
                            """
                            (function(){
                                var s=document.createElement('style');
                                s.id='muses-compat-patch';
                                s.textContent='.amll-lyric-player{mix-blend-mode:normal !important} .amll-lyric-player{--amll-lp-color:white !important}';
                                if(!document.getElementById('muses-compat-patch')) document.head.appendChild(s);
                                document.documentElement.style.setProperty('--amll-lp-color','white');
                            })();
                            """.trimIndent(),
                            null
                        )
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        android.util.Log.w("LyricWeb", "onReceivedError code=${error?.errorCode} desc=${error?.description} url=${request?.url}")
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        android.util.Log.w("LyricWeb", "${message.message()} @${message.lineNumber()} [${message.messageLevel()}]")
                        return true
                    }
                }

                // 捕获 webView 引用供 bridge 乐观更新
                val webViewRef = this
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onLineClick(lineIndex: Int, startTime: Double) {
                            val seek = startTime.toLong()
                            onSeekState.value.invoke(seek)
                            // 乐观更新：立即让 JS 跳到目标时间，防旧时间回滚（DroidMate 同款）
                            webViewRef.post {
                                webViewRef.evaluateJavascript(
                                    "window.updateTime && window.updateTime($seek);",
                                    null
                                )
                            }
                        }

                        @JavascriptInterface
                        fun onPageReady() {
                            webViewRef.post { isPageReady = true }
                        }

                        @JavascriptInterface
                        fun onInteractionStart() {
                            onInteractionStart()
                        }

                        @JavascriptInterface
                        fun onInteractionEnd() {
                            onInteractionEnd()
                        }

                        @JavascriptInterface
                        fun log(message: String, level: String) {
                            android.util.Log.w("LyricWeb", "[js:$level] $message")
                        }
                    },
                    "Android"
                )
                loadUrl("https://appassets.androidplatform.net/assets/amll/index.html")
            }
        },
        update = { view ->
            if (!isPageReady) return@AndroidView

            // 播放状态去重
            val curIsPlaying = isPlayingState.value.invoke()
            if (lastIsPlaying != curIsPlaying) {
                lastIsPlaying = curIsPlaying
                view.evaluateJavascript(
                    "window.setPaused && window.setPaused(${!curIsPlaying});",
                    null
                )
            }

            // 时间节流
            val now = System.currentTimeMillis()
            if (now - lastTimeUpdate >= frameIntervalMs) {
                val t = positionMsState.value.invoke()
                view.evaluateJavascript("window.updateTime && window.updateTime($t);", null)
                lastTimeUpdate = now
            }

            // 字号（仅变化时注入，避免每帧重设）
            if (lastFontSize != fontSizeSp) {
                lastFontSize = fontSizeSp
                view.evaluateJavascript(
                    "document.documentElement.style.setProperty('--amll-lp-font-size','${fontSizeSp}px');",
                    null
                )
            }

            // 歌词引用比较去重
            if (lyrics !== lastLyrics) {
                if (lyrics != null && lyricJson != null) {
                    view.evaluateJavascript("window.updateLyrics && window.updateLyrics($lyricJson);", null)
                } else {
                    view.evaluateJavascript("window.updateLyrics && window.updateLyrics({\"lines\":[]});", null)
                }
                lastLyrics = lyrics
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.clearHistory()
            view.clearCache(true)
            view.removeJavascriptInterface("Android")
            view.destroy()
        }
    )
}

/** SyncedLyrics → DroidMate 格式 JSON（{"lines":[...]}），前端 updateLyrics 用
 * 预处理：避免时间重叠行触发 web 版 assertValidLyricTimestamps 失败；确保 end>start 防 NaN
 */
internal fun SyncedLyrics.toLyricLinesJson(showTranslation: Boolean): String? {
    if (lines.isEmpty()) return null
    val sb = StringBuilder("{\"lines\":[")
    var cursorMs = 0L
    lines.forEachIndexed { index, line ->
        if (index > 0) sb.append(',')
        val originalStart = line.start.toLong()
        val originalEnd = line.end.toLong()
        val safeStart = if (originalStart < cursorMs) cursorMs else originalStart
        val safeEnd = if (originalEnd <= safeStart) safeStart + 1L else originalEnd
        cursorMs = safeEnd
        when (line) {
            is KaraokeLine -> {
                val words = line.syllables.joinToString(",") { s ->
                    val ws = s.start.toLong().coerceAtLeast(safeStart)
                    val we = s.end.toLong().coerceAtMost(safeEnd).coerceAtLeast(ws + 1L)
                    "{\"startTime\":$ws,\"endTime\":$we,\"word\":${s.content.toJsStringLiteral()}}"
                }
                val text = line.syllables.joinToString("") { it.content }
                sb.append("{\"words\":[$words],")
                sb.append("\"text\":${text.toJsStringLiteral()},")
                sb.append("\"translatedLyric\":${(if (showTranslation) line.translation else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"romanLyric\":${(if (showTranslation) line.phonetic else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"startTime\":$safeStart,\"endTime\":$safeEnd,")
                sb.append("\"isBG\":${line is KaraokeLine.AccompanimentKaraokeLine},")
                sb.append("\"isDuet\":${line.alignment == KaraokeAlignment.End}}")
            }

            is SyncedLine -> {
                val ws = line.start.toLong().coerceAtLeast(safeStart)
                val we = line.end.toLong().coerceAtMost(safeEnd).coerceAtLeast(ws + 1L)
                sb.append("{\"words\":[{\"startTime\":$ws,\"endTime\":$we,\"word\":${line.content.toJsStringLiteral()}}],")
                sb.append("\"text\":${line.content.toJsStringLiteral()},")
                sb.append("\"translatedLyric\":${(if (showTranslation) line.translation else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"romanLyric\":\"\",")
                sb.append("\"startTime\":$safeStart,\"endTime\":$safeEnd,\"isBG\":false,\"isDuet\":false}")
            }
        }
    }
    sb.append("]}")
    return sb.toString()
}

/** 转成 JS 双引号字符串字面量 */
internal fun String.toJsStringLiteral(): String {
    val sb = StringBuilder("\"")
    for (c in this) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c.code < 0x20) {
                sb.append("\\u")
                sb.append(c.code.toString(16).padStart(4, '0'))
            } else {
                sb.append(c)
            }
        }
    }
    sb.append('"')
    return sb.toString()
}
