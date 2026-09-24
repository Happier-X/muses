package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult

/**
 * AI 推荐的有效配置（全部自填，走 **OpenAI 兼容**协议：`{baseUrl}/chat/completions`）。
 *
 * 不再内置服务商预设：服务地址/模型/API Key 全部由用户手动填写，
 * 兼容任意 OpenAI 兼容服务（DeepSeek/Kimi/GLM/通义/Ollama/vLLM/自建中转）。
 *
 * [apiKey] 不落 DataStore：由调用方从加密凭据库取出后传入（见 CredentialsRepository）。
 */
data class AiRecommendConfig(
    /** 服务地址（OpenAI 兼容 baseUrl，含版本段，如 https://host/v1） */
    val baseUrl: String = "",
    /** 模型名 */
    val model: String = "",
    val apiKey: String = "",
) {
    /** 实际请求地址（已去尾斜杠） */
    val resolvedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    /** 实际请求模型 */
    val resolvedModel: String
        get() = model.trim()

    /** 是否具备发起请求的最小条件（缺一即不可用） */
    val isUsable: Boolean
        get() = apiKey.isNotBlank() && resolvedBaseUrl.isNotBlank() && resolvedModel.isNotBlank()
}

/** AI 给出的一条建议（**尚未**落地为可播曲目） */
data class AiSongSuggestion(
    val name: String,
    val artist: String?,
    /** AI 给的一句推荐理由（可空；展示在列表次要行） */
    val reason: String? = null,
)

/** 已落地：建议 + 匹配到的平台真实曲目（可直接进播放链路） */
data class AiRecommendedTrack(
    val suggestion: AiSongSuggestion,
    val result: OnlineSearchResult,
)

/**
 * 推荐结果。
 *
 * [suggested] 与 [tracks] 的差值即「AI 编造/平台搜不到」被丢弃的量，
 * UI 据此如实告知（如「AI 推荐 20 首，匹配到 13 首」），不掩盖失真。
 */
data class AiRecommendResult(
    val tracks: List<AiRecommendedTrack>,
    val suggested: Int,
    val unmatched: List<AiSongSuggestion>,
) {
    val matched: Int get() = tracks.size
    val allUnmatched: Boolean get() = tracks.isEmpty() && suggested > 0
}

/** AI 调用/解析失败（网络、鉴权、限流、返回不可用） */
class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * AI API Key 在凭据库（CredentialsRepository，密文存储）中的 sourceId。
 *
 * 设置页写入、首页读取**共用此常量**：两边各写一遍字符串一旦不一致，
 * 就会出现「设置页显示已保存、首页却认为未配置」的难题（难查）。
 */
const val AI_API_KEY_SOURCE_ID = "ai-recommend"
