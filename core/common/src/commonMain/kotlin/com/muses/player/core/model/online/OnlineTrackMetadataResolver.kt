package com.muses.player.core.model.online

/**
 * 在线曲目封面/歌词获取端口（对应洛雪自定义源脚本的 `pic` / `lyric` 动作）。
 *
 * ## 为什么与 [OnlineTrackResolver] 分开
 * 直链解析（`musicUrl`）发生在**播放打开时**，由播放器内部触发，属播放链路；
 * 封面/歌词是**展示元数据**，由播放页在曲目切换时取用，属 UI 链路。
 * 两者的触发时机与失败语义都不同（拿不到封面/歌词不应影响播放），故拆成两个端口，
 * 由 `:core:lxsdk` 分别适配（[OnlineTrackResolver] 已在用）。
 *
 * ## 契约
 * - 数据源：音源脚本声明的 `pic` / `lyric` 动作；脚本未声明该动作时返回 null；
 * - **不缓存**：缓存策略归调用方（播放页当前按「切歌重新取」处理）；
 * - **失败静默**：解析失败/不支持一律返回 null，不向 UI 抛错；仅协程取消向上传播。
 */
interface OnlineTrackMetadataResolver {

    /**
     * 取封面远程 URL（脚本 `pic` 动作）。
     * @return http(s) 直链；脚本不支持、无数据或失败时 null
     */
    suspend fun resolveCover(ref: OnlineTrackRef): String?

    /**
     * 取歌词四字段（脚本 `lyric` 动作）。
     * @return 原样透传的 [OnlineTrackLyrics]；脚本不支持、无数据或失败时 null
     */
    suspend fun resolveLyrics(ref: OnlineTrackRef): OnlineTrackLyrics?
}

/**
 * 在线曲目歌词（洛雪四字段原样透传）。
 *
 * 字段语义与洛雪 `lyric` 动作一致；**解析与对齐**（转成 [com.muses.player.core.lyrics.model.LyricsDocument]）
 * 归 `:core:common` 的 `LxLyricParser`，本模型只负责过桥，避免 lxsdk 反向依赖歌词模型。
 */
data class OnlineTrackLyrics(
    /** 原始歌词（行级 LRC） */
    val lyric: String? = null,
    /** 翻译歌词（行级 LRC） */
    val tlyric: String? = null,
    /** 罗马音歌词（行级 LRC） */
    val rlyric: String? = null,
    /** 逐字歌词（`[mm:ss.xxx]<startMs,durationMs>字`） */
    val lxlyric: String? = null,
) {
    val isEmpty: Boolean
        get() = lyric.isNullOrBlank() && tlyric.isNullOrBlank() &&
            rlyric.isNullOrBlank() && lxlyric.isNullOrBlank()
}

/**
 * 空实现：宿主未装配在线音源（或未接 `:core:lxsdk`）时使用。
 *
 * 有了它，播放页无需判空端口，也不会因为「没装洛雪脚本」而在切歌时崩掉。
 */
object NoOpOnlineTrackMetadataResolver : OnlineTrackMetadataResolver {
    override suspend fun resolveCover(ref: OnlineTrackRef): String? = null
    override suspend fun resolveLyrics(ref: OnlineTrackRef): OnlineTrackLyrics? = null
}
