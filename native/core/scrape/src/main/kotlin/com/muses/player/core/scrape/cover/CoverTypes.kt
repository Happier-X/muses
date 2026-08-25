package com.muses.player.core.scrape.cover

// 规格书 = src/features/cover/types.ts（逐类型翻译）

/** 在线封面匹配查询（types.ts OnlineCoverQuery） */
data class OnlineCoverQuery(
    val songId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
)

/** 在线封面来源；wire 值与 Web 保持一致（types.ts OnlineCoverSource） */
enum class OnlineCoverSource(val wire: String) {
    ITUNES("itunes"),
    KW("kw"),
    MG("mg"),
    KG("kg"),
    TX("tx"),
    WY("wy"),
}

/** 封面匹配失败原因；wire 值对齐 Web 字符串（types.ts OnlineCoverMatchFail） */
enum class OnlineCoverMatchFailReason(val wire: String) {
    NO_MATCH("no-match"),
    NETWORK("network"),
    ABORTED("aborted"),
}

/** 六源链封面匹配结果（OnlineCoverMatchOk | OnlineCoverMatchFail 的密封建模） */
sealed interface OnlineCoverMatchResult {
    /** 命中：返回远程封面 URL（不落盘）与来源 */
    data class Ok(
        val remoteUrl: String,
        val source: OnlineCoverSource,
    ) : OnlineCoverMatchResult

    data class Fail(val reason: OnlineCoverMatchFailReason) : OnlineCoverMatchResult
}

/** 单源封面 provider：返回远程封面 URL，无结果返回 null；网络/解析错误抛出由 matcher 归类 */
interface CoverProvider {
    val id: OnlineCoverSource

    suspend fun searchCoverUrl(query: OnlineCoverQuery): String?
}
