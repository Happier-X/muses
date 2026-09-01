package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 整页单一 WebView — 承载背景 + 左侧信息栏 + 右侧歌词
 * 前端为 app/src/main/assets/amll/full-player.js + amll.bundle.js (DroidMate)
 * 复用 LyricWebView 的 HARDWARE + isPageReady + 手势暂停 + file→dataURL 链路
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullPlayerWebView(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    coverUri: String?,
    positionMs: () -> Int,
    durationMs: () -> Long,
    isPlaying: () -> Boolean,
    repeatMode: () -> Int,
    shuffleEnabled: () -> Boolean,
    lyrics: SyncedLyrics?,
    showTranslation: Boolean,
    fontSizeSp: Float,
    activePanel: Int,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    onPanelChange: (Int) -> Unit,
    onLyricAtTopChange: (Boolean) -> Unit = {},
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var isPageReady by remember { mutableStateOf(false) }
    var lastLyrics by remember { mutableStateOf<SyncedLyrics?>(null) }
    var lastCoverUri by remember { mutableStateOf<String?>(null) }
    var lastTitle by remember { mutableStateOf<String?>(null) }
    var lastArtist by remember { mutableStateOf<String?>(null) }
    var lastActivePanel by remember { mutableStateOf<Int?>(null) }
    var hasConfiguredMotion by remember { mutableStateOf(false) }
    var webViewHolder by remember { mutableStateOf<WebView?>(null) }
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollResumeJob by remember { mutableStateOf<Job?>(null) }
    var isLyricAtTop by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val onSeekState = rememberUpdatedState(onSeek)
    val onPlayPauseState = rememberUpdatedState(onPlayPause)
    val onPreviousState = rememberUpdatedState(onPrevious)
    val onNextState = rememberUpdatedState(onNext)
    val onToggleRepeatState = rememberUpdatedState(onToggleRepeat)
    val onToggleShuffleState = rememberUpdatedState(onToggleShuffle)
    val onOpenQueueState = rememberUpdatedState(onOpenQueue)
    val onOpenEditMetaState = rememberUpdatedState(onOpenEditMeta)
    val onPanelChangeState = rememberUpdatedState(onPanelChange)
    val isPlayingState = rememberUpdatedState(isPlaying)
    val positionMsState = rememberUpdatedState(positionMs)
    val durationMsState = rememberUpdatedState(durationMs)
    val repeatModeState = rememberUpdatedState(repeatMode)
    val shuffleState = rememberUpdatedState(shuffleEnabled)
    val activePanelState = rememberUpdatedState(activePanel)

    val frameIntervalMs = 32L
    val lyricJson = remember(lyrics, showTranslation) { lyrics?.toLyricLinesJson(showTranslation) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView.setWebContentsDebuggingEnabled(true)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                var downX = 0f
                var downY = 0f
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                setOnTouchListener { v, event ->
                    val wv = v as WebView
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x; downY = event.y
                            scrollResumeJob?.cancel()
                            // DOWN 暂不判定，MOVE 时按 dy/dx 决定 isUserScrolling 与拦截
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = abs(event.x - downX)
                            val dy = abs(event.y - downY)
                            // 垂直为主（dy>dx）：信息页直接放行给外层关闭；歌词页在顶部时放行（关闭），未在顶部时由 WebView 滚动
                            if (dy > dx && dy > touchSlop) {
                                val isLyricPanel = activePanelState.value == 1
                                if (isLyricPanel) {
                                    if (isLyricAtTop) {
                                        android.util.Log.w("FullPlayer", "MOVE vertical lyric atTop -> parent")
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    } else {
                                        android.util.Log.w("FullPlayer", "MOVE vertical lyric notTop -> webview")
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                        if (!isUserScrolling) { isUserScrolling = true; wv.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:false});", null) }
                                    }
                                } else {
                                    android.util.Log.w("FullPlayer", "MOVE vertical info -> parent")
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            } else if (dx > dy && dx > touchSlop) {
                                android.util.Log.w("FullPlayer", "MOVE horizontal -> webview")
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                if (!isUserScrolling) { isUserScrolling = true; wv.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:false});", null) }
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            scrollResumeJob?.cancel()
                            val target = wv
                            scrollResumeJob = scope.launch {
                                delay(3000)
                                isUserScrolling = false
                                target.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:true});", null)
                            }
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                        if (isPageReady) isPageReady = false
                        if (lastLyrics != null) lastLyrics = null
                        if (lastCoverUri != null) lastCoverUri = null
                        hasConfiguredMotion = false
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        view.evaluateJavascript(
                            """
                            (function(){
                                var s=document.createElement('style');
                                s.id='muses-compat-patch';
                                s.textContent='.amll-lyric-player{--amll-lp-color:white !important}';
                                if(!document.getElementById('muses-compat-patch')) document.head.appendChild(s);
                                document.documentElement.style.setProperty('--amll-lp-color','white');
                                window.configureLyricMotion && window.configureLyricMotion({enableBlur:true, enableScale:true, enableSpring:true});
                            })();
                            """.trimIndent(), null
                        )
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                        android.util.Log.w("FullPlayer", "${m.message()} @${m.lineNumber()} [${m.messageLevel()}]")
                        return true
                    }
                }
                val ref = this
                addJavascriptInterface(object {
                    @JavascriptInterface fun onPageReady() { ref.post { isPageReady = true } }
                    @JavascriptInterface fun onLineClick(lineIndex: Int, startTime: Double) {
                        val seek = startTime.toLong()
                        ref.post {
                            onSeekState.value.invoke(seek)
                            ref.evaluateJavascript("window.updateTime && window.updateTime($seek);", null)
                        }
                    }
                    @JavascriptInterface fun onAction(json: String) {
                        android.util.Log.w("FullPlayer", "onAction $json")
                        ref.post {
                            try {
                                val obj = org.json.JSONObject(json)
                                when (obj.optString("action")) {
                                    "playPause" -> { android.util.Log.w("FullPlayer", "-> playPause"); onPlayPauseState.value.invoke() }
                                    "previous" -> { android.util.Log.w("FullPlayer", "-> previous"); onPreviousState.value.invoke() }
                                    "next" -> { android.util.Log.w("FullPlayer", "-> next"); onNextState.value.invoke() }
                                    "toggleRepeat" -> { android.util.Log.w("FullPlayer", "-> toggleRepeat"); onToggleRepeatState.value.invoke() }
                                    "toggleShuffle" -> { android.util.Log.w("FullPlayer", "-> toggleShuffle"); onToggleShuffleState.value.invoke() }
                                    "openQueue" -> { android.util.Log.w("FullPlayer", "-> openQueue"); onOpenQueueState.value.invoke() }
                                    "openMore" -> { android.util.Log.w("FullPlayer", "-> openMore"); onOpenEditMetaState.value.invoke() }
                                    "seekTo" -> {
                                        val pos = obj.optLong("positionMs", 0L)
                                        android.util.Log.w("FullPlayer", "-> seekTo $pos")
                                        onSeekState.value.invoke(pos)
                                        ref.evaluateJavascript("window.updateTime && window.updateTime($pos);", null)
                                    }
                                    "close" -> onClose()
                                    else -> android.util.Log.w("FullPlayer", "-> unknown ${obj.optString("action")}")
                                }
                            } catch (e: Exception) { android.util.Log.w("FullPlayer", "onAction parse err ${e.message} $json") }
                        }
                    }
                    @JavascriptInterface fun onPanelChange(index: Int) { onPanelChangeState.value.invoke(index) }
                    @JavascriptInterface fun onLyricScroll(isAtTop: Boolean) { isLyricAtTop = isAtTop; android.util.Log.w("FullPlayer", "onLyricScroll isAtTop="+isAtTop); onLyricAtTopChange(isAtTop) }
                    @JavascriptInterface fun log(msg: String, level: String) { android.util.Log.w("FullPlayer", "[js:$level] $msg") }
                }, "Android")
                // 兼容旧 bridge 的直接方法名（full-player.js 的 bindClick 会尝试多种）
                addJavascriptInterface(object {
                    @JavascriptInterface fun onPlayPause() { onPlayPauseState.value.invoke() }
                    @JavascriptInterface fun onNext() { onNextState.value.invoke() }
                    @JavascriptInterface fun onPrevious() { onPreviousState.value.invoke() }
                }, "AndroidDirect")
                loadUrl("https://appassets.androidplatform.net/assets/amll/index.html")
                webViewHolder = this
            }
        },
        update = { view ->
            webViewHolder = view
            if (!isPageReady) return@AndroidView

            // 标题/歌手/封面
            if (lastTitle != title || lastArtist != artist) {
                lastTitle = title; lastArtist = artist
                val escTitle = title.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                val escArtist = artist.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                view.evaluateJavascript("window.updateInfo && window.updateInfo({title:\"$escTitle\", artist:\"$escArtist\"});", null)
            }
            if (coverUri != lastCoverUri) {
                lastCoverUri = coverUri
                if (!coverUri.isNullOrBlank()) {
                    when {
                        coverUri.startsWith("file://") || coverUri.startsWith("/") -> {
                            val path = if (coverUri.startsWith("file://")) coverUri.removePrefix("file://") else coverUri
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val file = File(path)
                                    if (file.exists()) {
                                        val bytes = file.readBytes()
                                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                        val mime = when (opts.outMimeType) { "image/png" -> "image/png"; "image/webp" -> "image/webp"; else -> "image/jpeg" }
                                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        val dataUrl = "data:$mime;base64,$b64"
                                        withContext(Dispatchers.Main) {
                                            val esc = dataUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"}); window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            val esc = coverUri.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"}); window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        val esc = coverUri.replace("\\", "\\\\").replace("\"", "\\\"")
                                        view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"});", null)
                                    }
                                }
                            }
                        }
                        coverUri.startsWith("content://") -> {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val cr = view.context.contentResolver
                                    cr.openInputStream(android.net.Uri.parse(coverUri))?.use { ins ->
                                        val bytes = ins.readBytes()
                                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        val dataUrl = "data:image/jpeg;base64,$b64"
                                        withContext(Dispatchers.Main) {
                                            val esc = dataUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"}); window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        val esc = coverUri.replace("\\", "\\\\").replace("\"", "\\\"")
                                        view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"});", null)
                                    }
                                }
                            }
                        }
                        else -> {
                            val esc = coverUri.replace("\\", "\\\\").replace("\"", "\\\"")
                            view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"$esc\"}); window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                        }
                    }
                } else {
                    view.evaluateJavascript("window.updateInfo && window.updateInfo({coverUrl:\"\"}); window.updateAlbumArt && window.updateAlbumArt('');", null)
                }
            }
            if (!hasConfiguredMotion) {
                hasConfiguredMotion = true
                view.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:true, enableScale:true, enableSpring:true});", null)
            }
            // 面板
            if (lastActivePanel != activePanel) {
                lastActivePanel = activePanel
                view.evaluateJavascript("window.setActivePanel && window.setActivePanel($activePanel);", null)
            }
            // 歌词
            if (lyrics !== lastLyrics) {
                if (lyrics != null && lyricJson != null) view.evaluateJavascript("window.updateLyrics && window.updateLyrics($lyricJson);", null)
                else view.evaluateJavascript("window.updateLyrics && window.updateLyrics({\"lines\":[]});", null)
                lastLyrics = lyrics
            }
        },
        onRelease = { v ->
            v.stopLoading(); v.clearHistory(); v.clearCache(true)
            v.removeJavascriptInterface("Android"); v.removeJavascriptInterface("AndroidDirect")
            v.destroy()
        }
    )

    // 播放进度与时间（手势时暂停，避免抢夺）
    LaunchedEffect(isPageReady, isUserScrolling) {
        while (true) {
            val view = webViewHolder
            if (isPageReady && view != null && !isUserScrolling) {
                val pos = positionMsState.value.invoke()
                val dur = durationMsState.value.invoke()
                val playing = isPlayingState.value.invoke()
                val rep = repeatModeState.value.invoke()
                val shuf = shuffleState.value.invoke()
                view.evaluateJavascript("window.updateProgress && window.updateProgress({position:$pos, duration:$dur, isPlaying:$playing, repeatMode:$rep, shuffleEnabled:$shuf});", null)
                view.evaluateJavascript("window.updateTime && window.updateTime($pos);", null)
                view.evaluateJavascript("window.setPaused && window.setPaused(${!playing});", null)
            }
            delay(frameIntervalMs)
        }
    }
}
