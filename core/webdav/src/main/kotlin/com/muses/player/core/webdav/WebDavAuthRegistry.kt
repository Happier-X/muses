package com.muses.player.core.webdav

import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * WebDAV 播放认证注册表：内存持有「baseUrl → Basic 凭据」映射，
 * 供 OkHttp 认证 interceptor 为播放流播请求注入 Authorization header。
 *
 * - [refresh] 全量加载音源列表并逐源解密密码；密码加载失败（未存储/解密异常）的源跳过；
 * - 密码仅在内存中短生命周期存在，不落日志/持久化（沿用项目安全惯例）；
 * - 匹配规则：请求 URL 以某注册源 baseUrl 为前缀（'/' 边界对齐），取最长匹配。
 */
class WebDavAuthRegistry constructor(
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
) {

    /** 单条注册项（baseUrl 已归一化） */
    private data class RegisteredSource(
        val baseUrl: String,
        val username: String?,
        val password: String,
    )

    @Volatile
    private var registered: List<RegisteredSource> = emptyList()

    /** 首次查询前懒加载标记；refresh 成功后置 true */
    @Volatile
    private var loaded: Boolean = false

    /**
     * 全量重新加载：Web 端音源增删改后由调用方触发，保持内存表与库一致。
     */
    suspend fun refresh() {
        val sources = sourceRepository.observeSources().first()
            .filter { it.type == SourceType.WEBDAV && !it.url.isNullOrBlank() }
        registered = sources.mapNotNull { source ->
            // 密码缺失/解密失败的源跳过：无凭据的请求只会换来 401，交给播放失败文案兜底
            val password = runCatching { credentialsRepository.getPassword(source.id) }
                .getOrNull() ?: return@mapNotNull null
            RegisteredSource(
                baseUrl = normalizeBaseUrl(requireNotNull(source.url)),
                username = source.username,
                password = password,
            )
        }
        loaded = true
    }

    /**
     * 返回该 URL 应携带的 Authorization header 值；无匹配注册源返回 null。
     *
     * 未加载时阻塞式全量加载一次：本方法只在 OkHttp interceptor（IO 线程）执行，
     * 不在主线程调用；权衡为避免引入异步预热链路，代价是首个请求多一次同步读库。
     */
    fun authorizationHeader(url: String): String? {
        if (!loaded) {
            runBlocking { refresh() }
        }
        val normalized = normalizeBaseUrl(url)
        val match = registered
            .filter { matchesPrefix(normalized, it.baseUrl) }
            .maxByOrNull { it.baseUrl.length } ?: return null
        // user 缺失按空 user 处理（Basic 冒号前置空）；显式 UTF-8 兼容非 ASCII 用户名
        // P2c 去 okhttp3：纯函数拼 Basic 头（与 KtorWebDavClient.basicHeader 同一语义）
        return KtorWebDavClient.basicHeader(match.username ?: "", match.password)
    }

    /** 前缀匹配需落在 '/' 边界，避免 `/dav` 误命中 `/davious` */
    private fun matchesPrefix(url: String, baseUrl: String): Boolean =
        url == baseUrl || url.startsWith("$baseUrl/")

    companion object {
        /**
         * baseUrl 归一化：trim 尾部 '/'，scheme+host 小写；path 部分保留原始大小写
         * （NAS 目录路径大小写敏感，host 不敏感）。
         */
        internal fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim().trimEnd('/')
            val schemeSeparator = trimmed.indexOf("://")
            if (schemeSeparator <= 0) return trimmed
            val scheme = trimmed.substring(0, schemeSeparator).lowercase()
            val rest = trimmed.substring(schemeSeparator + 3)
            val host = rest.substringBefore('/').lowercase()
            val path = rest.substringAfter('/', "")
            return if (path.isEmpty()) "$scheme://$host" else "$scheme://$host/$path"
        }
    }
}
