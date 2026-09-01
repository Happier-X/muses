package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import java.io.File
import kotlin.math.abs
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    albumArtUri: String? = null,
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
    var hasConfiguredMotion by remember { mutableStateOf(false) }
    var lastAlbumArtUri by remember { mutableStateOf<String?>(null) }

    val onSeekState = rememberUpdatedState(onSeek)
    val isPlayingState = rememberUpdatedState(isPlaying)
    val positionMsState = rememberUpdatedState(positionMs)

    // 节流：60Hz 16ms，兼顾逐词平滑与 UI 线程
    val frameIntervalMs = 32L
    var webViewHolder by remember { mutableStateOf<WebView?>(null) }
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollResumeJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

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
                // 手势时暂停自动滚动，AMLL 默认手离开后约 3s 回弹到当前行；同时处理与 HorizontalPager 的手势冲突
                var downX = 0f
                var downY = 0f
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                setOnTouchListener { v, event ->
                    val wv = v as WebView
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                            if (!isUserScrolling) {
                                isUserScrolling = true
                                // 手势开始：暂停自动滚动并临时清晰化，避免轻滚即糊
                                wv.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:false});", null)
                            }
                            scrollResumeJob?.cancel()
                            onInteractionStart()
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isUserScrolling) {
                                isUserScrolling = true
                                wv.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:false});", null)
                                scrollResumeJob?.cancel()
                                onInteractionStart()
                            }
                            val dx = abs(event.x - downX)
                            val dy = abs(event.y - downY)
                            if (dy > dx && dy > touchSlop) {
                                // 垂直滚动为主：禁止父层 HorizontalPager 拦截
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            } else if (dx > dy && dx > touchSlop) {
                                // 水平滑动为主：允许父层处理翻页
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            scrollResumeJob?.cancel()
                            val target = wv
                            scrollResumeJob = scope.launch {
                                delay(3000)
                                isUserScrolling = false
                                // 手势结束 3s 后恢复沉浸式模糊与自动滚动
                                target.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:true});", null)
                                onInteractionEnd()
                            }
                        }
                    }
                    false
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
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
                        if (lastAlbumArtUri != null) lastAlbumArtUri = null
                        hasConfiguredMotion = false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        // 确保 --amll-lp-color 有值，否则 mask 渐变依赖的根变量为空
                        view.evaluateJavascript(
                            """
                            (function(){
                                var s=document.createElement('style');
                                s.id='muses-compat-patch';
                                s.textContent='.amll-lyric-player{--amll-lp-color:white !important}';
                                if(!document.getElementById('muses-compat-patch')) document.head.appendChild(s);
                                document.documentElement.style.setProperty('--amll-lp-color','white');
                                // 初始保留完整 AMLL 沉浸效果（模糊+缩放+弹簧），手势期间再动态清晰化
                                window.configureLyricMotion && window.configureLyricMotion({enableBlur:true, enableScale:true, enableSpring:true});
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
                webViewHolder = this
            }
        },
        update = { view ->
            webViewHolder = view
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

            // 时间由下方 LaunchedEffect 驱动，此处仅保留字号/歌词去重，时间不再在 update 块节流（update 仅重组时触发）

            // 字号（仅变化时注入，避免每帧重设）
            if (lastFontSize != fontSizeSp) {
                lastFontSize = fontSizeSp
                view.evaluateJavascript(
                    "document.documentElement.style.setProperty('--amll-lp-font-size','${fontSizeSp}px');",
                    null
                )
            }

            // 首次就绪后配置一次动效：保留完整模糊/缩放/弹簧，轻滚时的清晰化由手势动态切换
            if (isPageReady && !hasConfiguredMotion) {
                hasConfiguredMotion = true
                view.evaluateJavascript("window.configureLyricMotion && window.configureLyricMotion({enableBlur:true, enableScale:true, enableSpring:true});", null)
            }

            // 专辑封面驱动 AMLL 流体背景（BackgroundRender），file:// 在 Kotlin 侧转 dataURL 再下发（1:1 DroidMate convertFileUriToDataUrl）
            if (isPageReady && albumArtUri != lastAlbumArtUri) {
                lastAlbumArtUri = albumArtUri
                val uri = albumArtUri
                android.util.Log.w("LyricWeb", "updateAlbumArt uri=${uri?.take(120)}")
                if (!uri.isNullOrBlank()) {
                    when {
                        uri.startsWith("file://") || uri.startsWith("/") -> {
                            val path = if (uri.startsWith("file://")) uri.removePrefix("file://") else uri
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val file = File(path)
                                    if (file.exists()) {
                                        val bytes = file.readBytes()
                                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                        val mime = when (opts.outMimeType) {
                                            "image/png" -> "image/png"
                                            "image/webp" -> "image/webp"
                                            else -> "image/jpeg"
                                        }
                                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        val dataUrl = "data:$mime;base64,$b64"
                                        withContext(Dispatchers.Main) {
                                            val esc = dataUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            val esc = uri.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        val esc = uri.replace("\\", "\\\\").replace("\"", "\\\"")
                                        view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                    }
                                }
                            }
                        }
                        uri.startsWith("content://") -> {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val cr = view.context.contentResolver
                                    cr.openInputStream(android.net.Uri.parse(uri))?.use { ins ->
                                        val bytes = ins.readBytes()
                                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        val dataUrl = "data:image/jpeg;base64,$b64"
                                        withContext(Dispatchers.Main) {
                                            val esc = dataUrl.replace("\\", "\\\\").replace("\"", "\\\"")
                                            view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                        }
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) {
                                        val esc = uri.replace("\\", "\\\\").replace("\"", "\\\"")
                                        view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                                    }
                                }
                            }
                        }
                        else -> {
                            val esc = uri.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                            view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt(\"$esc\");", null)
                        }
                    }
                } else {
                    view.evaluateJavascript("window.updateAlbumArt && window.updateAlbumArt('');", null)
                }
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

    // 播放位置推进：32ms 粒度，逐词渐变所需（仿 DroidMate 的 frameInterval 节流）；手势滚动时暂停，避免与用户手动滚动抢夺
    LaunchedEffect(isPageReady, isUserScrolling) {
        while (true) {
            val view = webViewHolder
            if (isPageReady && view != null && !isUserScrolling) {
                val t = positionMsState.value.invoke()
                view.evaluateJavascript("window.updateTime && window.updateTime($t);", null)
            }
            delay(frameIntervalMs)
        }
    }

    // 播放/暂停状态去重由 update 块处理，时间循环仅负责 updateTime
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
