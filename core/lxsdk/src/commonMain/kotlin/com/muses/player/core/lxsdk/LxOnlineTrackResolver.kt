package com.muses.player.core.lxsdk

import com.muses.player.core.model.online.OnlinePlayableUrl
import com.muses.player.core.model.online.OnlineResolveException
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.model.online.OnlineTrackResolver
import com.muses.player.core.model.online.OnlineTrackCandidateProvider
import com.muses.player.core.model.online.OnlinePlayableUrlProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 把 [LxScriptRepository] 适配为播放链路的 [OnlineTrackResolver] 端口。
 *
 * 播放器（Android `PlayerConnection` / 桌面 `JvmPlayerPort`）只依赖 [:core:common]
 * 的端口接口，本适配器把洛雪引擎的具体调用封装在 [:core:lxsdk] 内，
 * 避免播放层反向依赖 JS 引擎。
 *
 * 直链**不做缓存**：洛雪直链普遍带时效签名，缓存会导致播放中途失效；
 * 每次播放/重播都重新解析（见端口契约）。
 */
class LxOnlineTrackResolver(
    private val repository: LxScriptRepository,
    /** 默认请求音质；null = 完全交给脚本自身的 mapQuality 兜底 */
    private val defaultQuality: LxQuality? = LxQuality.DEFAULT,
    private val candidateProvider: OnlineTrackCandidateProvider? = null,
    private val urlProbe: OnlinePlayableUrlProbe? = null,
) : OnlineTrackResolver {

    override suspend fun resolve(ref: OnlineTrackRef): OnlinePlayableUrl {
        if (candidateProvider == null) return resolveDirect(ref).also { OnlineTrackSession.rememberResolution(ref, ref) }
        return withTimeoutOrNull(50_000) { resolveWithFallback(ref) }
            ?: throw OnlineResolveException("寻找可播放音源超时，请稍后重试")
    }

    private suspend fun resolveWithFallback(ref: OnlineTrackRef): OnlinePlayableUrl {
        val platforms = repository.loadAll().filter { it.loadError == null }
            .flatMap { it.sources.filterValues { source -> source.supports(LxAction.MUSIC_URL) }.keys }.distinct()
        if (platforms.isEmpty()) throw OnlineResolveException("没有可用的在线音源脚本，请先导入脚本")
        var failure: Exception? = null
        if (ref.platform in platforms) {
            try {
                withTimeoutOrNull(12_000) { resolveDirect(ref) }?.let {
                    OnlineTrackSession.rememberResolution(ref, ref)
                    return it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        val candidates = candidateProvider!!.candidates(ref, platforms)
        for (candidate in candidates.take(4)) {
            try {
                withTimeoutOrNull(7_000) { resolveDirect(candidate) }?.let {
                    OnlineTrackSession.rememberResolution(ref, candidate)
                    return it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
        throw OnlineResolveException("当前音源无法播放，其他平台也未找到可播放的同曲版本", failure)
    }

    private suspend fun resolveDirect(ref: OnlineTrackRef): OnlinePlayableUrl {
        val quality = ref.quality?.let(LxQuality::fromKey) ?: defaultQuality
        return try {
            val result = repository.resolveMusicUrlInfo(
                platform = ref.platform,
                musicInfoJson = ref.musicInfoJson,
                quality = quality,
                acceptUrl = { url ->
                    urlProbe == null || withTimeoutOrNull(4_000) { urlProbe.canOpen(url) } == true
                },
            )
            OnlinePlayableUrl(url = result.url, quality = result.quality?.key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LxResolveException) {
            throw OnlineResolveException(e.message ?: "在线音源解析失败", e)
        } catch (e: LxException) {
            throw OnlineResolveException(e.message ?: "在线音源解析失败", e)
        } catch (e: Exception) {
            throw OnlineResolveException("在线音源解析异常：${e.message}", e)
        }
    }
}
