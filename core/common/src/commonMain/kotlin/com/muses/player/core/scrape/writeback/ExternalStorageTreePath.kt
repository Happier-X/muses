package com.muses.player.core.scrape.writeback

/** 已规范化物理路径在标准外部存储树授权内时，返回对应文档 ID。 */
fun externalStorageDocumentId(path: String, treeId: String, primaryRoot: String): String? {
    if (':' !in treeId) return null
    val volume = treeId.substringBefore(':')
    if (volume.isEmpty()) return null
    val relative = treeId.substringAfter(':').trim('/')
    if (relative.split('/').any { it == "." || it == ".." }) return null
    val root = if (volume.equals("primary", true)) primaryRoot.trimEnd('/') else "/storage/$volume"
    val directory = if (relative.isEmpty()) root else "$root/$relative"
    if (!path.startsWith("$directory/")) return null
    val fileRelative = path.removePrefix("$root/")
    if (fileRelative.split('/').any { it.isEmpty() || it == "." || it == ".." }) return null
    return "$volume:$fileRelative"
}
