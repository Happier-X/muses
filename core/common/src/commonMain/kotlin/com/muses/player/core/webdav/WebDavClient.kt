package com.muses.player.core.webdav

import java.io.File

/** WebDAV 条目（PROPFIND depth 1 返回的单个文件/目录） */
data class WebDavItem(
    val name: String,
    val url: String,
    val isDirectory: Boolean,
    val contentLength: Long = 0L,
    val lastModified: String? = null,
    val eTag: String? = null,
)

/** WebDAV 认证失败异常 */
class WebDavAuthException(message: String) : Exception(message)

/** WebDAV 请求失败异常 */
class WebDavRequestException(val code: Int, message: String) : Exception(message)

/**
 * WebDAV 客户端接口。
 *
 * 移植自旧工程 WebDavPlugin 的 PROPFIND/GET/PUT/DELETE/MOVE + Basic Auth，
 * 阶段 2 完整实现。
 *
 * W3 上收（任务 09-05-scrape-kmp R2/R4）：接口自 core:webdav 移入 :core:common commonMain
 * （同包名，core:webdav 经 api(:core:common) 解析，KtorWebDavClient 实现留守原地零改动）；
 * `get/put` 的 `java.io.File` 签名原样保留（android+jvm 双 JVM 系 target 均可用，
 * WebDavAudioCache/写回链路调用面零改动）。
 */
interface WebDavClient {
    /** 设置 Basic Auth 凭据（内存持有，不持久化密码到网络层） */
    fun authenticate(username: String, password: String)

    /** 校验服务端连通性（PROPFIND depth 0） */
    suspend fun probe(baseUrl: String): Boolean

    /** PROPFIND depth 1 列出目录内容 */
    suspend fun list(url: String): List<WebDavItem>

    /** 下载文件到本地 */
    suspend fun get(url: String, dest: File): File

    /** 上传文件 */
    suspend fun put(url: String, source: File)

    /** 删除远程文件/目录 */
    suspend fun delete(url: String)

    /** 移动/重命名 */
    suspend fun move(source: String, dest: String)

    /** 下载文件内容为字符串（用于 sidecar 歌词等文本文件） */
    suspend fun getString(url: String): String?
}
