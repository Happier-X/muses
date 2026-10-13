package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromeElevation
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 放入 Scaffold 悬浮按钮槽：当前歌曲离开可视区且停止滚动后显示，避让底部播放器。 */
@Composable
fun SongListJumpFab(listState: LazyListState, currentItemKey: Any?, onClick: () -> Unit) {
    var scrollSettled by remember { mutableStateOf(true) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) scrollSettled = false
        else {
            delay(300)
            scrollSettled = true
        }
    }
    val currentInViewport by remember(currentItemKey, listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.any { item ->
                item.key == currentItemKey && item.offset < info.viewportEndOffset &&
                    item.offset + item.size > info.viewportStartOffset
            }
        }
    }.collectAsState(false)
    if (currentItemKey == null || currentInViewport || !scrollSettled) return

    val scheme = MiuixTheme.colorScheme
    var slotBottomPx by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { slotBottomPx = it.boundsInWindow().bottom }) {
        val chromeTop = LocalBottomChromeElevation.current.value
        val slotBottom = with(LocalDensity.current) { slotBottomPx.toDp() }
        val clearance = if (slotBottomPx == 0f) LocalBottomChromePadding.current + 16.dp
            else slotBottom - chromeTop + 16.dp
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = clearance, end = 16.dp),
            minWidth = 44.dp,
            minHeight = 44.dp,
            containerColor = scheme.surfaceVariant,
        ) {
            Icon(TablerIcons.MyLocation, contentDescription = "跳转到当前播放",
                tint = scheme.onBackgroundVariant, modifier = Modifier.size(20.dp))
        }
    }
}
