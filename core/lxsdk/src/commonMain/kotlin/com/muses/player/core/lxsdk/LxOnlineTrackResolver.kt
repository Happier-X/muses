package com.muses.player.core.lxsdk

import com.muses.player.core.model.online.OnlinePlayableUrl
import com.muses.player.core.model.online.OnlineResolveException
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackResolver

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
) : OnlineTrackResolver {

    override suspend fun resolve(ref: OnlineTrackRef): OnlinePlayableUrl {
        val quality = ref.quality?.let(LxQuality::fromKey) ?: defaultQuality
        return try {
            val url = repository.resolveMusicUrl(
                platform = ref.platform,
                musicInfoJson = ref.musicInfoJson,
                quality = quality,
            )
            OnlinePlayableUrl(url = url, quality = quality?.key)
        } catch (e: LxResolveException) {
            throw OnlineResolveException(e.message ?: "在线音源解析失败", e)
        } catch (e: LxException) {
            throw OnlineResolveException(e.message ?: "在线音源解析失败", e)
        } catch (e: Exception) {
            throw OnlineResolveException("在线音源解析异常：${e.message}", e)
        }
    }
}
