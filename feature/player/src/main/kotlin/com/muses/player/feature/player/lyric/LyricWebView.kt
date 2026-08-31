package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.JavascriptInterface
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
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.delay

/**
 * 基于 WebView 的 AMLL 网页版歌词渲染。
 *
 * 直接运行 Web 版 LyricPlayer（assets/lyrics/，@applemusic-like-lyrics/core 打包），
 * 滚动弹簧/波浪/缩放等动画效果与 AMLL 网页版 100% 一致。
 *
 * 数据流：
 * - lyrics（SyncedLyrics）→ JSON 注入 setLyric；
 * - 播放位置 positionMs() 每 50ms 注入 setCurrentTime；
 * - 行点击 → 桥 onSeek（原生 seek）；触摸 → onInteractionStart/End（唤起控制条）。
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

    val bridge = remember {
        object {
            @JavascriptInterface
            fun onSeek(ms: Double) {
                onSeek(ms.toLong())
            }

            @JavascriptInterface
            fun onInteractionStart() {
                onInteractionStart()
            }

            @JavascriptInterface
            fun onInteractionEnd() {
                onInteractionEnd()
            }
        }
    }

    val lyricJson = remember(lyrics, showTranslation) {
        lyrics?.toLyricLinesJson(showTranslation)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        webView = view
                        view.evaluateJavascript("window.AMLLHost.setFontSize($fontSizeSp);", null)
                        lyricJson?.let { json ->
                            view.evaluateJavascript(
                                "window.AMLLHost.setLyric(${json.toJsStringLiteral()}, ${positionMs()});",
                                null
                            )
                        }
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                        android.util.Log.w("LyricWeb", "${message.message()} @${message.lineNumber()}")
                        return true
                    }
                }
                addJavascriptInterface(bridge, "AndroidLyric")
                loadUrl("file:///android_asset/lyrics/index.html")
            }
        },
        update = { view -> webView = view }
    )

    // 歌词变化 → 注入（等待 WebView 就绪；onPageFinished 时歌词可能尚未解析）
    LaunchedEffect(lyrics, lyricJson) {
        var view = webView
        while (view == null) {
            delay(100)
            view = webView
        }
        val json = lyricJson
        if (json != null) {
            view.evaluateJavascript(
                "window.AMLLHost.setLyric(${json.toJsStringLiteral()}, ${positionMs()});",
                null
            )
        } else {
            view.evaluateJavascript("window.AMLLHost.clearLyric();", null)
        }
    }

    // 字号调整
    LaunchedEffect(fontSizeSp) {
        webView?.evaluateJavascript("window.AMLLHost.setFontSize($fontSizeSp);", null)
    }

    // 播放位置推进（50ms 粒度，逐词渐变所需）
    LaunchedEffect(Unit) {
        while (true) {
            val view = webView
            if (view != null) {
                view.evaluateJavascript(
                    "window.AMLLHost.setCurrentTime(${positionMs()}, false);",
                    null
                )
                view.evaluateJavascript(
                    "window.AMLLHost.setPlaying(${isPlaying()});",
                    null
                )
            }
            delay(50)
        }
    }
}

/** SyncedLyrics → AMLL LyricLine[] JSON（web 版数据格式）
 * 预处理：避免时间重叠行触发 web 版 assertValidLyricTimestamps 失败（元数据行「作词/作曲/编曲」
 * 经常同 startTime=0）；为每行 startTime 添加微递增确保单调非重叠。
 */
internal fun SyncedLyrics.toLyricLinesJson(showTranslation: Boolean): String? {
    if (lines.isEmpty()) return null
    val sb = StringBuilder("[")
    var cursorMs = 0L
    lines.forEachIndexed { index, line ->
        if (index > 0) sb.append(',')
        val originalStart = line.start
        val originalEnd = line.end
        val safeStart = if (originalStart < cursorMs) cursorMs else originalStart
        val safeEnd = if (originalEnd <= safeStart) safeStart + 1L else originalEnd
        cursorMs = safeEnd
        when (line) {
            is KaraokeLine -> {
                val offset = safeStart - originalStart
                val words = line.syllables.joinToString(",") { s ->
                    "{\"startTime\":${s.start + offset},\"endTime\":${(s.end + offset).coerceAtLeast(s.start + offset + 1)},\"word\":${s.content.toJsStringLiteral()}}"
                }
                sb.append("{\"words\":[$words],")
                sb.append("\"translatedLyric\":${(if (showTranslation) line.translation else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"romanLyric\":${(if (showTranslation) line.phonetic else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"startTime\":$safeStart,\"endTime\":$safeEnd,")
                sb.append("\"isBG\":${line is KaraokeLine.AccompanimentKaraokeLine},")
                sb.append("\"isDuet\":${line.alignment == KaraokeAlignment.End}}")
            }

            is SyncedLine -> {
                val offset = safeStart - originalStart
                sb.append("{\"words\":[{\"startTime\":${line.start + offset},\"endTime\":${(line.end + offset).coerceAtLeast(line.start + offset + 1)},\"word\":${line.content.toJsStringLiteral()}}],")
                sb.append("\"translatedLyric\":${(if (showTranslation) line.translation else null).orEmpty().toJsStringLiteral()},")
                sb.append("\"romanLyric\":\"\",")
                sb.append("\"startTime\":$safeStart,\"endTime\":$safeEnd,\"isBG\":false,\"isDuet\":false}")
            }
        }
    }
    sb.append(']')
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
