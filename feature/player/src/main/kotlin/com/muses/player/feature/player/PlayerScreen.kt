package com.muses.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.feature.player.lyric.AmllWebView
import com.muses.player.feature.player.lyric.coverUriToAppAssetsUrl
import org.json.JSONObject

/**
 * 播放页 —— P4.4 全 WebView 方案。
 *
 * 背景：Compose 复刻版反复出现布局错位（偏左/半屏分割），旧 Capacitor 版全 WebView 从无此类问题；
 * 用户决策整个播放页用一个 WebView 承载（复用 AmllWebView 的 assetLoader/软件层/lifecycle 治理），
 * 队列页保持原生命航（openQueue 走桥回调跳转原生路由）。
 *
 * 桥协议：
 * - Native→JS：window.updatePlayerState(json)（标题/歌手/封面/isPlaying/position/duration/
 *   buffering/repeat/shuffle/翻译态 + 状态栏/导航栏避让 px）；歌词载荷沿用 updateLyrics/updatePosition 通道；
 * - JS→Native：nativeBridge.onAction(json)（playPause/next/previous/seekTo/setRepeatMode/
 *   setShuffle/toggleTranslation/openQueue/close），AmllWebView 内部已 post 主线程。
 *
 * 状态下行节流：position 由 ViewModel ~500ms 轮询驱动重组，其余状态低频变化，
 * 直接以「重组驱动 JSON 重建」替代显式 sample——注入频率与旧轮询同量级，无需额外节流。
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onOpenQueue: () -> Unit = {},
    /** M3：打开编辑歌曲信息弹窗（宿主在 MusesApp 层承载全局 EditMetaSheet） */
    onOpenEditMeta: () -> Unit = {},
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val lyricsJson by viewModel.lyricsJson.collectAsStateWithLifecycle()
    val hasTranslation by viewModel.hasTranslation.collectAsStateWithLifecycle()
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val stickyCover by viewModel.stickyCover.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val density = LocalDensity.current
    // 平板分支判定：对齐 PlayerPage.vue isTabletLayout（viewportWidth>=768 且宽>高横屏）
    val configuration = LocalConfiguration.current
    val isTabletLayout = remember(configuration) {
        configuration.screenWidthDp >= 768 && configuration.screenHeightDp < configuration.screenWidthDp
    }
    // 安全区：WebView 内 env(safe-area-inset-*) 恒为 0，经 payload 注入 px 值给前端 CSS 变量
    val insetTopPx = WindowInsets.statusBars.getTop(density)
    val insetBottomPx = WindowInsets.navigationBars.getBottom(density)

    // ---- 播放页状态 JSON（重组驱动重建；title 空串 = 无播放歌曲，前端显示空态）----
    val playerStateJson = remember(
        currentMediaItem, isPlaying, position, duration, repeatMode,
        shuffleModeEnabled, isBuffering, stickyCover, hasTranslation, translationEnabled,
        insetTopPx, insetBottomPx, isTabletLayout,
    ) {
        buildPlayerStateJson(
            title = if (currentMediaItem == null) "" else {
                currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
            },
            artist = currentMediaItem?.mediaMetadata?.artist?.toString(),
            coverUrl = coverUriToAppAssetsUrl(
                uri = stickyCover,
                cacheDirPath = context.cacheDir.absolutePath,
            ),
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
            buffering = isBuffering,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleModeEnabled,
            hasTranslation = hasTranslation,
            translationEnabled = translationEnabled,
            insetTopPx = insetTopPx,
            insetBottomPx = insetBottomPx,
            isTabletLayout = isTabletLayout,
        )
    }

    // ---- 全屏 WebView 容器（沉浸底色 #05070d 与前端 .drag-layer 一致，防加载闪白）----
    // 若 WebView 仍黑屏，底层 Box 的原生回退 UI 保底可见
    Box(modifier.fillMaxSize().background(Color(0xFF05070D))) {
        AmllWebView(
            modifier = Modifier.fillMaxSize(),
            payloadJson = lyricsJson,
            positionMsFlow = viewModel.lyricPosition,
            isPlaying = viewModel.isPlaying,
            playerStateJson = playerStateJson,
            onBridgeAction = { json ->
                handleBridgeAction(
                    json, viewModel, onClose, onOpenQueue,
                    onOpenEditMeta,
                )
            }
        )
        // 原生回退 UI：WebView 黑屏时仍可用（封面 + 标题 + 控制），与 WebView 底层共存，层级在 WebView 之上但不遮挡手势（点击可透传至 WebView 的关闭按钮区域除外）
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 72.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val cover = stickyCover
            if (!cover.isNullOrEmpty()) {
                com.muses.player.core.ui.components.SaltCover(uri = cover, size = 240.dp, radius = com.muses.player.core.ui.components.SaltCoverRadius.MD)
                Spacer(modifier = Modifier.size(24.dp))
            }
            Text(
                text = currentMediaItem?.mediaMetadata?.title?.toString()?.ifEmpty { "暂无播放歌曲" } ?: "暂无播放歌曲",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.size(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "上一曲", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                androidx.compose.material3.IconButton(onClick = { viewModel.playPause() }) {
                    Icon(imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                androidx.compose.material3.IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "下一曲", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
        // 限流错误条：复用 PlaybackRecoveryController.playbackError（含 429 的「触发限流，稍后重试」）
        if (playbackError != null) {
            androidx.compose.material3.Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.retryPlayback() }) {
                        Text("重试", color = Color.White)
                    }
                },
                containerColor = Color(0xCC1A1A1A),
                contentColor = Color.White,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(playbackError!!, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp).clickable { viewModel.clearPlaybackError() },
                    )
                }
            }
        }
    }
}

// ---------- 状态下行 JSON 构建 ----------

/** media3 repeatMode → 前端枚举（'off' | 'one' | 'all'） */
private fun repeatModeLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ONE -> "one"
    Player.REPEAT_MODE_ALL -> "all"
    else -> "off"
}

/**
 * 构建窗口.updatePlayerState 载荷 JSON（org.json 保证字符串转义正确）。
 * coverUrl=null 用 JSONObject.NULL 显式输出 null 字面量——前端以 !== null 判定粘性沿用。
 */
private fun buildPlayerStateJson(
    title: String,
    artist: String?,
    coverUrl: String?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    buffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    insetTopPx: Int,
    insetBottomPx: Int,
    /** 横屏平板分支（≥768dp 且宽>高）：前端切 .pp-tablet 类，对照 player-page--tablet */
    isTabletLayout: Boolean,
): String = JSONObject().apply {
    put("title", title)
    put("artist", artist ?: JSONObject.NULL)
    put("coverUrl", coverUrl ?: JSONObject.NULL)
    put("isPlaying", isPlaying)
    put("positionMs", positionMs)
    put("durationMs", durationMs)
    put("buffering", buffering)
    put("repeatMode", repeatModeLabel(repeatMode))
    put("shuffleEnabled", shuffleEnabled)
    put("hasTranslation", hasTranslation)
    put("translationEnabled", translationEnabled)
    put("insetTopPx", insetTopPx)
    put("insetBottomPx", insetBottomPx)
    put("isTabletLayout", isTabletLayout)
}.toString()

// ---------- JS→Native 动作分派 ----------

/** 解析 nativeBridge.onAction JSON 并分派 ViewModel 动作 / 页面回调（已在主线程） */
private fun handleBridgeAction(
    json: String,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit = {},
) {
    val action = runCatching { JSONObject(json) }.getOrNull() ?: return
    when (action.optString("action")) {
        "playPause" -> viewModel.playPause()
        "next" -> viewModel.skipToNext()
        "previous" -> viewModel.skipToPrevious()
        // 桥用一次性 seek（拖动 preview 在前端本地完成，抬起才发）
        "seekTo" -> viewModel.seekTo(action.optLong("positionMs"))
        // 对齐 Vue 语义：one ↔ all 二态切换
        "setRepeatMode" -> viewModel.setRepeatMode(
            if (action.optString("mode") == "one") Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL,
        )
        "setShuffle" -> viewModel.setShuffleModeEnabled(action.optBoolean("enabled"))
        "toggleTranslation" -> viewModel.toggleTranslation()
        // 队列页保持原生命航：跳转原生 Queue 路由
        "openQueue" -> onOpenQueue()
        // M3：编辑歌曲信息（原生 EditMetaSheet 三维云搜）
        "openEditMeta" -> onOpenEditMeta()
        "close" -> onClose()
    }
}

/** 队列页 —— QueuePage.vue 一比一翻译（P4.4 保持原生命航，未改动） */
@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val currentIndex = queue.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LocalSaltColors.current.surface),
    ) {
        // __header：标题 + 清空/关闭
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SaltSpacing.spacing, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row {
                // 对齐 Vue v-if="queueState.hasItems"：空队列不显示清空按钮
                if (queue.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清空队列",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.clearQueue() },
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭队列",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onClose),
                )
            }
        }

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier
                            .background(
                                if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                            )
                            // Web .queue-page__row border-bottom 1px hairline（暗色沉浸页用白系低透明近似）
                            .drawBehind {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.08f),
                                    topLeft = Offset(0f, size.height - 1f),
                                    size = Size(size.width, 1f),
                                )
                            },
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playAtIndex(index) }
                                .padding(horizontal = SaltSpacing.spacing, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "未知歌曲",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                (index + 1).toString(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "从队列删除",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.removeQueueItemAt(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}
