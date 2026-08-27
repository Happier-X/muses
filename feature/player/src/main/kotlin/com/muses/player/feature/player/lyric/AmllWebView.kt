package com.muses.player.feature.player.lyric

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

/** 前端模块就绪握手动作（P4.4：修复 onPageFinished 早于 ES module 执行导致首轮注入丢失的黑屏） */
private const val BRIDGE_ACTION_READY = "{\"action\":\"ready\"}"

/** AMLL WebView 页面地址（经 WebViewAssetLoader 以 https 源安全加载 APK assets） */
private const val AMLL_START_URL = "https://appassets.androidplatform.net/assets/amll/index.html"

/**
 * 把库内封面 URI 转为 WebView 内可加载的 URL。
 *
 * WebView 页面源是 https（appassets.androidplatform.net），file:// 封面会被混合内容策略拦截，
 * 故本地文件统一映射为 https://appassets.androidplatform.net/cache/... 由 [CacheDirPathHandler] 提供服务。
 * data:/http(s) 原样透传；无法映射时返回 null（前端粘性沿用上一张封面）。
 */
fun coverUriToAppAssetsUrl(uri: String?, cacheDirPath: String): String? = when {
    uri == null -> null
    uri.startsWith("file://") -> {
        val path = uri.removePrefix("file://")
        if (path.startsWith(cacheDirPath)) {
            "https://appassets.androidplatform.net/cache/" + path.removePrefix(cacheDirPath).trimStart('/')
        } else {
            // 非 cache 目录的本地封面暂不映射（M1 封面均落 cache/covers）
            null
        }
    }
    uri.startsWith("http://") || uri.startsWith("https://") || uri.startsWith("data:") -> uri
    else -> null
}

/** 服务 cacheDir 下静态文件的 PathHandler（含目录穿越防护） */
private class CacheDirPathHandler(private val root: File) : WebViewAssetLoader.PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        val file = File(root, path)
        if (!file.canonicalPath.startsWith(root.canonicalPath + File.separator)) return null
        if (!file.isFile) return null
        return try {
            WebResourceResponse(file.guessImageMimeType(), null, file.inputStream())
        } catch (_: Exception) {
            null
        }
    }

    private fun File.guessImageMimeType(): String = when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }
}

/**
 * JS→Native 动作桥：前端经 window.nativeBridge.onAction(json) 调用。
 * 回调发生在 WebView 的 JS 线程，[onAction] 内部自行 post 主线程后再触碰 Compose 状态。
 */
private class NativeBridge(private val delegate: (String) -> Unit) {
    @JavascriptInterface
    fun onAction(json: String) {
        delegate(json)
    }
}

/**
 * AMLL 歌词 + 流体背景宿主：一个 WebView 页面承担双职责（design.md §3.1/§3.2）。
 * P4.4 起同时是沉浸式播放页的全屏容器（播放页 UI DOM 由同一页面承载）。
 *
 * 生命周期契约（继承 spec/frontend/features-player.md，原生等价实现）：
 * - 与歌词状态解耦：无词时 payload.lines 为空数组，BackgroundRender 照常渲染；
 * - 切后台 ON_STOP → window.pauseRender()；恢复 ON_START → resumeRender()。
 *   **禁止**用销毁/重建 WebView 控制暂停（复刻 Web 层 v-if 教训）；
 * - 粘性封面由 PlayerViewModel 保证：coverUrl=null 时前端不清空旧封面；
 * - 进度注入依赖上游 ~100ms 节流的 [positionMsFlow]，仅播放中发送，播完钳制也在上游完成。
 *
 * TODO(优化项)：WebView 实例 App 级单例复用——当前在 Composable 内创建，
 * 播放页常驻场景下重建频率低可接受；后续如需彻底避免重建成本再提升作用域。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AmllWebView(
    modifier: Modifier = Modifier,
    payloadJson: String?,
    positionMsFlow: StateFlow<Long>,
    isPlaying: StateFlow<Boolean>,
    /** 非空时页面 ready / 载荷变化即注入 window.updatePlayerState(<json>)（P4.4 播放页状态下行） */
    playerStateJson: String? = null,
    /** 前端 nativeBridge.onAction 原始 JSON（已在内部 post 主线程），由调用方解析分派 */
    onBridgeAction: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewHolder = remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var jsReady by remember { mutableStateOf(false) }

    // 桥回调经 ref 转发：factory 闭包只捕获一次，后续重组更新 lambda 不重建 WebView
    val onBridgeActionRef = remember { mutableStateOf(onBridgeAction) }
    onBridgeActionRef.value = onBridgeAction
    // 最新载荷 ref：ready 握手时重推用（避免闭包捕获过期值）
    val playerStateRef = remember { mutableStateOf(playerStateJson) }
    playerStateRef.value = playerStateJson
    val payloadRef = remember { mutableStateOf(payloadJson) }
    payloadRef.value = payloadJson

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // 慢速全文档绘制：使 WebView 内容可被 PixelCopy/screencap 捕获（硬件加速 SurfaceView 在部分模拟器上截屏为黑）
            try { WebView.enableSlowWholeDocumentDraw() } catch (_: Exception) {}
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                // CDP 远程调试（chrome://inspect / adb forward），排查渲染问题后可关
                WebView.setWebContentsDebuggingEnabled(true)
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d("AmllWebView", "JS: ${consoleMessage.message()} -- ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}")
                        return true
                    }
                }
                // 保持默认硬件加速：P4.4 全页 WebView 后 LAYER_TYPE_SOFTWARE 会导致
                // 大尺寸表面（含 WebGL/PIXI）整层不上屏——真机与模拟器均表现为纯底色"黑屏"。
                // 若个别模拟器复现 WebGL 不上屏，属其 GPU 合成兼容性问题，勿再全局切软件层

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .addPathHandler("/cache/", CacheDirPathHandler(ctx.cacheDir))
                    .build()

                // JS→Native 动作桥：NativeBridge 回调在 WebView JS 线程，此处统一 post 主线程，
                // 保证调用方 lambda 可安全触碰 Compose 状态 / ViewModel
                val mainHandler = Handler(Looper.getMainLooper())
                addJavascriptInterface(
                    NativeBridge { json ->
                        mainHandler.post {
                            if (json == BRIDGE_ACTION_READY) {
                                Log.d("AmllWebView", "JS ready handshake")
                                jsReady = true
                                // 前端 module 就绪握手：onPageFinished 时 ES module 尚未执行完，
                                // 首轮 updatePlayerState/updateLyrics 注入会静默丢失——这里全量重推
                                playerStateRef.value?.let {
                                    Log.d("AmllWebView", "ready inject updatePlayerState")
                                    webViewHolder.value?.evaluateJavascript(
                                        "try{window.updatePlayerState(${AmllMapper.quote(it)}); 'ok:'+document.getElementById('player-ui')?.hidden}catch(e){'err:'+e}",
                                    ) { res -> Log.d("AmllWebView", "ready inject result=$res") }
                                }
                                payloadRef.value?.let {
                                    webViewHolder.value?.evaluateJavascript(
                                        "window.updateLyrics(${AmllMapper.quote(it)})",
                                        null,
                                    )
                                }
                            } else {
                                onBridgeActionRef.value(json)
                            }
                        }
                    },
                    "nativeBridge",
                )

                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val resp = assetLoader.shouldInterceptRequest(request.url)
                        if (resp == null) {
                            Log.w("AmllWebView", "shouldIntercept miss: ${request.url}")
                        }
                        return resp
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("AmllWebView", "onPageFinished: $url")
                        pageReady = true
                    }


                }

                loadUrl(AMLL_START_URL)
            }.also { webViewHolder.value = it }
        },
        onRelease = { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
        },
    )

    // 页面就绪或切歌（payload 变化）→ 注入歌词载荷；songId token 防过期回调由前端校验
    LaunchedEffect(webViewHolder.value, jsReady, payloadJson) {
        val wv = webViewHolder.value ?: return@LaunchedEffect
        if (!jsReady || payloadJson == null) return@LaunchedEffect
        // 前端契约为 updateLyrics(payload: string)，内部 JSON.parse——必须以 JS 字符串字面量嵌入，
        // 直接内插对象字面量会被 ToString 成 "[object Object]" 导致解析失败
        Log.d("AmllWebView", "inject updateLyrics")
        wv.evaluateJavascript("window.updateLyrics(${AmllMapper.quote(payloadJson)})", null)
    }

    // 进度注入：上游已按 ~100ms 节流且仅在播放中发射；暂停即停发
    LaunchedEffect(webViewHolder.value, pageReady) {
        val wv = webViewHolder.value ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        combine(positionMsFlow, isPlaying) { pos, playing -> pos to playing }
            .distinctUntilChanged()
            .collect { (posMs, playing) ->
                if (playing) {
                    wv.evaluateJavascript("window.updatePosition($posMs)", null)
                }
            }
    }

    // 播放页状态下行（P4.4）：JS 就绪后载荷变化即注入；JSON 由调用方构建，
    // 此处同样以 JS 字符串字面量嵌入（quote()），前端内部 JSON.parse
    LaunchedEffect(webViewHolder.value, jsReady, playerStateJson) {
        val wv = webViewHolder.value ?: return@LaunchedEffect
        if (!jsReady || playerStateJson == null) return@LaunchedEffect
        wv.evaluateJavascript("window.updatePlayerState(${AmllMapper.quote(playerStateJson)})", null)
    }

    // 渲染循环生命周期治理：切后台 pause / 恢复 resume，WebView 不销毁重建
    DisposableEffect(lifecycleOwner, webViewHolder.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP ->
                    webViewHolder.value?.evaluateJavascript("if(window.pauseRender) window.pauseRender()", null)
                Lifecycle.Event.ON_START ->
                    webViewHolder.value?.evaluateJavascript("if(window.resumeRender) window.resumeRender()", null)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
