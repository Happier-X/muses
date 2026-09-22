package com.muses.player.feature.sources

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WebDAV 目录浏览结果跨页会话 —— 对照 Web 层 webdavBrowseSession.ts。
 *
 * 浏览页确认后写入，表单页消费（take 语义：读走即清空）。
 * 仅内存持有（密码不落盘），进程死亡即失效——与 Web 层行为一致。
 *
 * 结果以 [StateFlow] 暴露，而不是只提供一次性读取：miuix-nav 在浏览页显示期间会保留
 * 表单页的组合，返回时表单页**不重新组合**，`LaunchedEffect(Unit)` 这类「进入组合只跑一次」
 * 的副作用会漏掉结果（表现为「点添加没反应，第二次进来才提示成功」）。表单页改为持续
 * collect 本 flow，结果一到就消费，与组合是否重建无关。
 */
object WebDavBrowseResultHolder {

    /** 浏览确认结果：选中路径 + 发起浏览时的连接信息 */
    data class BrowseResult(
        val paths: List<String>,
        val serverUrl: String,
        val username: String,
        val password: String,
    )

    private val _result = MutableStateFlow<BrowseResult?>(null)

    /** 供表单页持续观察（当前值 + 后续变更） */
    val result: StateFlow<BrowseResult?> = _result.asStateFlow()

    fun set(value: BrowseResult) {
        _result.value = value
    }

    /** 读走并清空（对照 takeWebDavBrowseResult） */
    fun take(): BrowseResult? {
        val taken = _result.value
        _result.value = null
        return taken
    }
}
