package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult

/**
 * AI 服务商预设（全部走 **OpenAI 兼容**协议：`{baseUrl}/chat/completions`）。
 *
 * 用兼容协议而非各家 SDK：一个客户端覆盖 DeepSeek/Kimi/GLM/通义/Ollama/vLLM，
 * 用户换服务商只需改下拉，不必改代码；`custom` 允许填任意自建/中转地址。
 *
 * baseUrl 均为各家公开文档的标准地址（**含版本段**，客户端只在其后拼 `/chat/completions`）。
 */
enum class AiProviderPreset(
    val key: String,
    val label: String,
    /** 默认地址；`custom` 为空串（必须由用户填写） */
    val baseUrl: String,
    val defaultModel: String,
    /** 下拉可选模型（允许用户手填其它模型名） */
    val models: List<String>,
) {
    DEEPSEEK(
        key = "deepseek",
        label = "DeepSeek 深度求索",
        baseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        models = listOf("deepseek-chat", "deepseek-reasoner"),
    ),
    KIMI(
        key = "kimi",
        label = "Kimi 月之暗面",
        baseUrl = "https://api.moonshot.cn/v1",
        defaultModel = "moonshot-v1-8k",
        models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"),
    ),
    GLM(
        key = "glm",
        label = "智谱 GLM",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModel = "glm-4-flash",
        models = listOf("glm-4-flash", "glm-4-air", "glm-4-plus"),
    ),
    QWEN(
        key = "qwen",
        label = "通义千问",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen-plus",
        models = listOf("qwen-turbo", "qwen-plus", "qwen-max"),
    ),
    CUSTOM(
        key = "custom",
        label = "自定义（OpenAI 兼容）",
        baseUrl = "",
        defaultModel = "",
        models = emptyList(),
    ),
    ;

    companion object {
        fun fromKey(key: String): AiProviderPreset =
            entries.firstOrNull { it.key == key } ?: DEEPSEEK
    }
}

/**
 * AI 推荐的有效配置（预设 + 用户覆盖 + API Key）。
 *
 * [apiKey] 不落 DataStore：由调用方从加密凭据库取出后传入（见 CredentialsRepository）。
 */
data class AiRecommendConfig(
    val providerKey: String,
    /** 自定义地址；空 = 用预设 */
    val baseUrl: String = "",
    /** 自定义模型；空 = 用预设默认 */
    val model: String = "",
    val apiKey: String = "",
) {
    val preset: AiProviderPreset get() = AiProviderPreset.fromKey(providerKey)

    /** 实际请求地址（已去尾斜杠） */
    val resolvedBaseUrl: String
        get() = (baseUrl.ifBlank { preset.baseUrl }).trimEnd('/')

    /** 实际请求模型 */
    val resolvedModel: String
        get() = model.ifBlank { preset.defaultModel }

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
