package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay

/**
 * 基于 WebView 的 AMLL 网页版歌词渲染（照抄 AMLL-DroidMate 完整方案）。
 *
 * 关键差异（vs 之前 file:// 版本）：
 * 1. WebViewAssetLoader：https://appassets.androidplatform.net/assets/amll/ 虚拟域名加载，
 *    规避 file:// 下 ES Module/CSS 的 CORS 限制（之前歌词 DOM 无样式/模块不执行的真因）；
 * 2. CSS 内联进 amll.bundle.js（vite cssInliner 插件），无外部 CSS 文件；
 * 3. 前端 API 照 DroidMate：window.updateLyrics({lines}) / updateTime(ms) / setPaused(bool)，
 *    LyricPlayer 内部 rAF 循环自驱动渲染（无内置循环的问题已由 DroidMate 的 tick 解决）。
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
    var webView by remember { mutableStateOf<WebView?>(null) }
    // 页面就绪信号：onPageReady 回调置 true，驱动 LaunchedEffect 注入（解决
    // onPageFinished 时 window.updateLyrics 尚未注册的竞态，照抄 DroidMate）
    var pageReady by remember { mutableStateOf(false) }

    val bridge = remember {
        object {
            @JavascriptInterface
            fun onLineClick(lineIndex: Int, startTime: Double) {
                onSeek(startTime.toLong())
            }

            @JavascriptInterface
            fun onPageReady() {
                pageReady = true
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
        }
    }

    val lyricJson = remember(lyrics, showTranslation) {
        lyrics?.toLyricLinesJson(showTranslation)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // 允许 Chrome DevTools 远程调试（排查 WebView 加载/JS 问题）
            WebView.setWebContentsDebuggingEnabled(true)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        webView = view
                        lyricJson?.let { json ->
                            view.evaluateJavascript(
                                "window.updateLyrics && window.updateLyrics($json);",
                                null
                            )
                        }
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
                        android.util.Log.w("LyricWeb", "${message.message()} @${message.lineNumber()}")
                        return true
                    }
                }
                addJavascriptInterface(bridge, "Android")
                loadUrl("https://appassets.androidplatform.net/assets/amll/index.html")
            }
        },
        update = { view -> webView = view }
    )

    // 歌词变化 / 页面就绪 → 注入（等待 WebView 就绪 + 页面就绪，
    // 解决 onPageFinished 时 window.updateLyrics 尚未注册的竞态，照抄 DroidMate）
    LaunchedEffect(lyrics, lyricJson, pageReady) {
        var view = webView
        while (view == null) {
            delay(100)
            view = webView
        }
        // 等到 page ready（window.updateLyrics 已注册）再注入
        while (!pageReady) {
            delay(50)
        }
        val json = lyricJson
        if (json != null) {
            view.evaluateJavascript("window.updateLyrics && window.updateLyrics($json);", null)
        } else {
            view.evaluateJavascript("window.updateLyrics && window.updateLyrics({\"lines\":[]});", null)
        }
    }

    // 播放位置推进（50ms 粒度，逐词渐变所需）
    LaunchedEffect(Unit) {
        while (true) {
            val view = webView
            if (view != null) {
                view.evaluateJavascript("window.updateTime && window.updateTime(${positionMs()});", null)
                view.evaluateJavascript("window.setPaused && window.setPaused(${!isPlaying()});", null)
            }
            delay(50)
        }
    }
}

/** SyncedLyrics → DroidMate 格式 JSON（{"metadata":..., "lines":[...]}，前端 updateLyrics 用）
 * 预处理：避免时间重叠行触发 web 版 assertValidLyricTimestamps 失败（元数据行「作词/作曲/编曲」
 * 经常同 startTime=0）；为每行 startTime 添加微递增确保单调非重叠。
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
