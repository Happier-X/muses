package com.muses.player.core.lxsdk.store

import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.lxsdk.LxScriptMetaParser
import java.io.File

/**
 * [LxScriptStore] 的文件实现（JVM / Android）。
 *
 * 落盘策略（对齐项目惯例：用户配置进 appDataDir，不进 cache）：
 * ```
 * <appDataDir>/lxscripts/<id>.js        脚本原文（UTF-8）
 * <appDataDir>/lxscripts/<id>.disabled  存在即表示已禁用（空标记文件）
 * ```
 * 用「标记文件」而非元数据索引：脚本原文即唯一事实来源，
 * 元信息（@name 等）每次读取时从头部注释解析，避免索引与原文不一致。
 *
 * 不使用 Room：脚本是低频、小体量、纯文本数据，文件直存更简单，
 * 也便于用户手动拷入/备份（与洛雪生态「导入 .js 文件」的心智一致）。
 */
class FileLxScriptStore(
    /**
     * 脚本目录供给。**必须懒求值**：Android 的 `PlatformDirs.appDataDir()` 依赖
     * `initPlatformDirs(context)` 先执行，若在构造时求值会在 Koin 解析链早期抛错
     * （实测崩溃：initPlatformDirs 未初始化就调用 appDataDir）。
     */
    private val rootDirProvider: () -> File = { File(PlatformDirs.appDataDir(), "lxscripts") },
) : LxScriptStore {

    /** 便捷构造：直接指定目录（测试/显式路径场景） */
    constructor(rootDir: File) : this(rootDirProvider = { rootDir })

    private val rootDir: File by lazy(rootDirProvider)

    private companion object {
        const val EXT_SCRIPT = ".js"
        const val EXT_DISABLED = ".disabled"
    }

    override fun list(): List<LxStoredScript> {
        val dir = rootDir.takeIf { it.isDirectory } ?: return emptyList()
        val files = (dir.listFiles { f -> f.isFile && f.name.endsWith(EXT_SCRIPT) } ?: return emptyList())
            .sortedBy { it.lastModified() }
        return files.mapNotNull { file ->
            val source = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                ?: return@mapNotNull null
            val id = file.name.removeSuffix(EXT_SCRIPT)
            val meta = LxScriptMetaParser.parse(source)
            LxStoredScript(
                id = id,
                // 元信息缺失时以文件名兜底（洛雪允许 @name 缺省）
                name = meta.name ?: id,
                source = source,
                meta = meta,
                importedAt = file.lastModified(),
                enabled = !disabledMarker(id).exists(),
            )
        }
    }

    override fun get(id: String): LxStoredScript? = list().firstOrNull { it.id == id }

    override fun save(id: String, source: String, enabled: Boolean): LxStoredScript {
        ensureRoot()
        scriptFile(id).writeText(source, Charsets.UTF_8)
        if (enabled) disabledMarker(id).delete() else disabledMarker(id).writeText("", Charsets.UTF_8)
        val meta = LxScriptMetaParser.parse(source)
        return LxStoredScript(
            id = id,
            name = meta.name ?: id,
            source = source,
            meta = meta,
            importedAt = scriptFile(id).lastModified(),
            enabled = enabled,
        )
    }

    override fun setEnabled(id: String, enabled: Boolean): Boolean {
        val file = scriptFile(id)
        if (!file.exists()) return false
        if (enabled) disabledMarker(id).delete() else disabledMarker(id).writeText("", Charsets.UTF_8)
        return true
    }

    override fun delete(id: String): Boolean {
        val removed = scriptFile(id).delete()
        disabledMarker(id).delete()
        return removed
    }

    private fun ensureRoot() {
        if (!rootDir.isDirectory) rootDir.mkdirs()
    }

    private fun scriptFile(id: String) = File(rootDir, "$id$EXT_SCRIPT")

    private fun disabledMarker(id: String) = File(rootDir, "$id$EXT_DISABLED")
}
