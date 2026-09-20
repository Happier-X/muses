package com.muses.player.core.lxsdk.store

import com.muses.player.core.lxsdk.LxScriptMeta

/**
 * 已导入的音源脚本（持久化条目）。
 *
 * [id] 为稳定标识（Muses 侧音源 id），用于在线曲目引用回指脚本；
 * [source] 为脚本原始文本（洛雪 `.js`，UTF-8）。
 */
data class LxStoredScript(
    val id: String,
    val name: String,
    val source: String,
    val meta: LxScriptMeta,
    val importedAt: Long,
    val enabled: Boolean = true,
)

/**
 * 音源脚本存储端口（平台无关）。
 *
 * 实现由各平台提供（JVM/Android = 文件；见 `FileLxScriptStore`）。
 * 抽成接口是为了让 UI 层（commonMain 的 feature 模块）能在不感知 `java.io` 的前提下
 * 完成脚本的增删查改。
 */
interface LxScriptStore {
    /** 列出全部已导入脚本（按导入时间升序） */
    fun list(): List<LxStoredScript>

    /** 按 id 读取单个脚本 */
    fun get(id: String): LxStoredScript?

    /** 导入/覆盖脚本；返回落盘后的条目 */
    fun save(id: String, source: String, enabled: Boolean = true): LxStoredScript

    /** 启用/禁用脚本（禁用后不参与直链解析，但不删除原文）；返回是否命中 */
    fun setEnabled(id: String, enabled: Boolean): Boolean

    /** 删除脚本；返回是否命中 */
    fun delete(id: String): Boolean
}
