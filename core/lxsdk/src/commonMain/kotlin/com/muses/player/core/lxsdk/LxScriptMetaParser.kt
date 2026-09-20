package com.muses.player.core.lxsdk

/**
 * 洛雪脚本头部元信息解析。
 *
 * 规范：文件开头必须包含块注释，内含 `@name`/`@description`/`@version`/`@author`/`@homepage`。
 * 实例（脚本头部块注释，含 @name / @version / @author 等标签）：
 * 行首为块注释起始符，内容形如 `@name 测试音乐源`，以块注释结束符收尾。
 *
 * 实现要点：
 * - 只扫描**首个块注释**（洛雪宿主同口径），支持尖叹号与双星两种起头；
 * - **逐行解析**：先剥掉行首的 `*` 装饰前缀，再匹配 `@tag value`。
 *   这样既能正确处理标准多行注释块，也天然避免把脚本正文里含 `@name` 的
 *   字符串/注释误当成元信息（不会跨行贪婪匹配）。
 */
object LxScriptMetaParser {

    /** 匹配首个块注释（非贪婪，跨行），捕获起始符之后的注释体 */
    private val BLOCK_COMMENT = Regex("""/\*[!*]?([\s\S]*?)\*/""")

    /** 单行标签：`@tag value`（要求有非空值，故 `@preserve` 这类裸标签不会命中） */
    private val LINE_TAG = Regex("""^@(\w+)\s+(.+)$""")

    /**
     * 解析脚本元信息。找不到块注释时返回全空 [LxScriptMeta]（不抛错——
     * 洛雪允许元信息缺失，此时以文件名为准，由上层决定是否拒绝导入）。
     */
    fun parse(source: String): LxScriptMeta {
        val block = BLOCK_COMMENT.find(source)?.groupValues?.getOrNull(1) ?: return LxScriptMeta()

        val tags = mutableMapOf<String, String>()
        for (rawLine in block.lines()) {
            // 剥掉块注释每行开头的 `*` 装饰与空白
            val line = rawLine.trim().removePrefix("*").trim()
            val match = LINE_TAG.find(line) ?: continue
            val tag = match.groupValues[1].lowercase()
            val value = match.groupValues[2].trim()
            // 同名标签取首个（洛雪同口径）
            if (value.isNotEmpty() && tag !in tags) tags[tag] = value
        }

        return LxScriptMeta(
            name = tags["name"],
            description = tags["description"],
            version = tags["version"],
            author = tags["author"],
            homepage = tags["homepage"],
        )
    }
}
