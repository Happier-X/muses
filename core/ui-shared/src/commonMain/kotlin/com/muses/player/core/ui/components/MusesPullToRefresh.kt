package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

/** 按 miuix 0.9.4 官方演示接入，仅统一中文文案，不改指示器、阈值和动画。 */
@Composable
fun MusesPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefresh(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        pullToRefreshState = rememberPullToRefreshState(),
        refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新…", "刷新完成"),
        content = content,
    )
}

/** 本地读取等挂起操作：立即进入刷新状态，结束或失败时释放，防止重复请求。 */
@Composable
fun MusesRefreshableContent(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val refresh by rememberUpdatedState(onRefresh)
    val scope = rememberCoroutineScope()
    MusesPullToRefresh(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                scope.launch {
                    try {
                        refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        MusesSnackbar.show("刷新失败，请重试")
                    } finally {
                        refreshing = false
                    }
                }
            }
        },
        modifier = modifier,
        content = content,
    )
}

/** 空态、错误态也提供滚动节点，让下拉手势能进入官方嵌套滚动链路。 */
@Composable
fun MusesRefreshablePlaceholder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).height(maxHeight)) {
            content()
        }
    }
}
