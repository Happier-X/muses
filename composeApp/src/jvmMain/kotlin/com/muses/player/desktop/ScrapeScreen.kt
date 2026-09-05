package com.muses.player.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.ScrapeCandidateRow
import com.muses.player.core.ui.components.SharedScrapeCandidate
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.desktop.di.DesktopContainer
import kotlinx.coroutines.launch

/**
 * 桌面刮削页（V3 刮削页共用化：设计 §3「按设置页模式共用化」决策落地）。
 *
 * 能力边界（诚实呈现，不做假交互）：
 * - 队列管理真实可用：队列存取走 ：core:common 的 commonMain 引擎 [ScrapeQueueStore]
 *   （DataStore KMP），歌曲信息经 [DesktopContainer] 反查 Room；
 * - 匹配/写回链（TextMetaMatcher/CoverMatcher/WritebackOrchestrator）在 ：core:scrape
 *   （安卓 library）内，桌面暂不可依赖 —— 经 [onMatchAll] 回调注入点预留，
 *   未注入时「全部开始」给出明确提示；引擎 KMP 化后注入真实现即可点亮。
 *
 * UI 全部消费 ：core:ui-shared 共用组件（ScrapeCandidateRow/SaltEmpty/SaltTextButton），
 * 纯 UI + 回调注入，对齐桌面 LibraryScreen 装配模式。
 */

/** 队列行展示数据（Room SongEntity 反查后的本地快照）。 */
private data class DesktopScrapeQueueRow(
    val songId: String,
    val title: String,
    val subtitle: String?,
    val coverUri: String?,
)

/**
 * 桌面刮削装配（进程内单例）。
 *
 * DataStore 用独立文件名：`createDataStore()` 默认文件（muses_settings）已被
 * DesktopCredentials 等各自实例化，同文件多 DataStore 实例会抛
 * 「multiple DataStores active」—— 刮削队列单独落文件彻底规避
 * （key `scrape_queue` 只在刮削文件内，跨端本就互不相通，无兼容影响）。
 */
private object DesktopScrapeGraph {
    val queueStore: ScrapeQueueStore by lazy {
        ScrapeQueueStore(
            dataStore = createDataStore(fileName = "muses_scrape.preferences_pb"),
            existingSongIds = {
                DesktopContainer.database().songDao().getAll().map { it.id }.toSet()
            },
        )
    }
}

@Composable
fun ScrapeScreen(
    modifier: Modifier = Modifier,
    /**
     * 匹配引擎回调注入点：桌面端 V1 未装配 ：core:scrape 匹配链，
     * null = 未接入（「全部开始」点击时提示能力边界）；引擎 KMP 化后由宿主注入。
     */
    onMatchAll: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()

    var rows by remember { mutableStateOf<List<DesktopScrapeQueueRow>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching {
                val ids = DesktopScrapeGraph.queueStore.load().map { it.songId }
                val db = DesktopContainer.database()
                rows = ids.mapNotNull { id ->
                    db.songDao().getById(id)?.let { e ->
                        DesktopScrapeQueueRow(
                            songId = e.id,
                            title = e.title,
                            subtitle = listOfNotNull(e.artist, e.albumTitle)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .takeIf { it.isNotBlank() },
                            coverUri = e.coverUri,
                        )
                    }
                }
            }.onFailure { e ->
                message = "读取刮削队列失败：${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "刮削",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "待刮削歌曲经队列统一补全标题/歌手/专辑/封面/歌词；桌面端当前提供队列管理，匹配引擎接入后开放一键刮削。",
            color = salt.text2,
            fontSize = 12.sp,
        )
        message?.let {
            Text(text = it, color = salt.danger, fontSize = 13.sp)
        }

        if (rows.isEmpty()) {
            SaltEmpty(
                title = "待刮削队列为空",
                description = "请先在歌曲页标记需要刮削的歌曲。",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(rows, key = { it.songId }) { row ->
                    ScrapeCandidateRow(
                        candidate = SharedScrapeCandidate(
                            songId = row.songId,
                            title = row.title,
                            subtitle = row.subtitle,
                            coverUri = row.coverUri,
                        ),
                        trailing = {
                            SaltTextButton(
                                text = "移除",
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            DesktopScrapeGraph.queueStore.remove(listOf(row.songId))
                                        }.onFailure { e ->
                                            message = "移除失败：${e.message}"
                                        }
                                        reload()
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        runCatching { DesktopScrapeGraph.queueStore.clear() }
                            .onFailure { e -> message = "清空失败：${e.message}" }
                        reload()
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("清空") }
            Button(
                onClick = {
                    val callback = onMatchAll
                    if (callback == null) {
                        message = "桌面端刮削引擎尚未接入：可先在队列中管理歌曲，匹配链待引擎 KMP 化后开放。"
                    } else {
                        callback()
                    }
                },
                modifier = Modifier.weight(2f),
                enabled = rows.isNotEmpty(),
            ) { Text("全部开始") }
        }
    }
}
