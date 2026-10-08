package com.muses.player.core.lxsdk.store

/** 随应用发布的免费音源；保持稳定 ID，文件库只做首次安装。 */
data class BundledLxScript(val id: String, val source: String, val sourceUrl: String)

object BundledLxScripts {
    private const val RESOURCE_ROOT = "/lxscripts/"

    fun load(): List<BundledLxScript> = resource("index.tsv").lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { line ->
            val fields = line.split('\t')
            require(fields.size == 3) { "内置音源清单格式错误" }
            BundledLxScript(fields[0], resource(fields[1]), fields[2])
        }.toList()

    private fun resource(name: String): String =
        checkNotNull(BundledLxScripts::class.java.getResourceAsStream(RESOURCE_ROOT + name)) {
            "缺少内置音源资源：$name"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
