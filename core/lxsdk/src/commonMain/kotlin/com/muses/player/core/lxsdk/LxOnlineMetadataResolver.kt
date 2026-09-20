package com.muses.player.core.lxsdk

import com.muses.player.core.model.online.OnlineTrackLyrics
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackRef
import kotlinx.coroutines.CancellationException

/**
 * 把 [LxScriptRepository] 的 `lyric` / `pic` 能力适配为 [OnlineTrackMetadataResolver] 端口。
 *
 * 与 [LxOnlineTrackResolver] 同理：播放页只依赖 `:core:common` 的端口，
 * 洛雪引擎的调用细节封装在 `:core:lxsdk` 内，避免 UI 层反向依赖 QuickJS。
 *
 * ## 失败语义
 * 端口契约要求「失败静默」（拿不到封面/歌词不应影响播放与其它 UI），
 * 因此这里把 [LxResolveException] / [LxException] 统一折算为 null，
 * **但协程取消必须原样抛出**——否则切歌时的取消请求会被当成「没歌词」而继续跑完整链路。
 */
class LxOnlineMetadataResolver(
    private val repository: LxScriptRepository,
) : OnlineTrackMetadataResolver {

    override suspend fun resolveCover(ref: OnlineTrackRef): String? = try {
        repository.resolveCover(ref.platform, ref.musicInfoJson).takeIf { it.isNotBlank() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    override suspend fun resolveLyrics(ref: OnlineTrackRef): OnlineTrackLyrics? = try {
        val lyric = repository.resolveLyric(ref.platform, ref.musicInfoJson)
        if (lyric.isEmpty) {
            null
        } else {
            OnlineTrackLyrics(
                lyric = lyric.lyric,
                tlyric = lyric.tlyric,
                rlyric = lyric.rlyric,
                lxlyric = lyric.lxlyric,
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
