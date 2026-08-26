package com.muses.player.feature.sources

/**
 * WebDAV 目录浏览结果跨页会话 —— 对照 Web 层 webdavBrowseSession.ts。
 *
 * 浏览页确认后写入，表单页返回重新组合时消费（take 语义：读走即清空）。
 * 仅内存持有（密码不落盘），进程死亡即失效——与 Web 层行为一致。
 */
object WebDavBrowseResultHolder {

    /** 浏览确认结果：选中路径 + 发起浏览时的连接信息 */
    data class BrowseResult(
        val paths: List<String>,
        val serverUrl: String,
        val username: String,
        val password: String,
    )

    @Volatile
    private var result: BrowseResult? = null

    fun set(value: BrowseResult) {
        result = value
    }

    /** 读走并清空（对照 takeWebDavBrowseResult） */
    fun take(): BrowseResult? {
        val taken = result
        result = null
        return taken
    }
}
