package com.muses.player.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** 不携带响应正文、请求参数或鉴权信息的失败类型。 */
enum class ProviderFailureKind(val message: String) {
    RATE_LIMITED("请求过于频繁，请稍后重试"),
    AUTH("鉴权失效或接口拒绝访问，请检查来源配置"),
    TIMEOUT("请求超时，请稍后重试"),
    SERVER("来源服务异常，请稍后重试"),
    RESPONSE("接口返回异常，请稍后重试"),
    NETWORK("网络连接失败，请检查网络后重试"),
    UNKNOWN("请求失败，请稍后重试"),
}

class ProviderHttpException(val status: Int) : IOException("http $status")
class ProviderResponseException : IOException("来源响应内容无效")
class ProviderTimeoutException : IOException("来源请求超时")

fun providerFailureKind(error: Exception): ProviderFailureKind = when (error) {
    is ProviderHttpException -> when (error.status) {
        429 -> ProviderFailureKind.RATE_LIMITED
        401, 403 -> ProviderFailureKind.AUTH
        408, 504 -> ProviderFailureKind.TIMEOUT
        in 500..599 -> ProviderFailureKind.SERVER
        else -> ProviderFailureKind.RESPONSE
    }
    is ProviderTimeoutException, is HttpRequestTimeoutException -> ProviderFailureKind.TIMEOUT
    is ProviderResponseException, is SerializationException -> ProviderFailureKind.RESPONSE
    is IOException -> ProviderFailureKind.NETWORK
    else -> ProviderFailureKind.UNKNOWN
}

/** 只跟随当前来源查询传播，避免把其他歌曲或来源的失败带入本次结果。 */
private class FailureCapture : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<FailureCapture>
    var failure: Exception? = null
}

/** 保留来源内部的备用接口回退；只有最终没有候选时，才重新抛出被内部吞掉的传输失败。 */
suspend fun <T> captureProviderFailures(block: suspend () -> T): T {
    val capture = FailureCapture()
    val result = withContext(capture) { block() }
    if (result == null || (result is Collection<*> && result.isEmpty())) capture.failure?.let { throw it }
    return result
}

/** 部分查询协议以 404 表示未收录，仅在调用者明确声明的请求作用域内豁免。 */
private object ExpectedProviderNotFound : AbstractCoroutineContextElement(Key) {
    object Key : CoroutineContext.Key<ExpectedProviderNotFound>
}
suspend fun <T> expectingProviderNotFound(block: suspend () -> T): T = withContext(ExpectedProviderNotFound) { block() }

suspend fun <T> observeProviderRequest(url: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // LRCLIB 的精确查询以 404 表示未收录，仍应继续搜索接口，不能显示为来源失败。
    val expectedNotFound = e is ProviderHttpException && e.status == 404 &&
        (currentCoroutineContext()[ExpectedProviderNotFound.Key] != null ||
            io.ktor.http.Url(url).let { it.host == "lrclib.net" && it.encodedPath == "/api/get" })
    if (!expectedNotFound) currentCoroutineContext()[FailureCapture]?.let { capture ->
        synchronized(capture) { if (capture.failure == null) capture.failure = e }
    }
    throw e
}
