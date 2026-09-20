package com.muses.player.core.lxsdk

import com.muses.player.core.lxsdk.crypto.LxCrypto
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient

/**
 * JVM/Android 平台的引擎工厂。
 *
 * `LxScriptEngine` 声明在 commonMain，但 crypto 的真实实现在 jvmShared（JCE），
 * 故由本层提供便捷构造——消费方（桌面/安卓）统一从此处创建，无需关心注入细节。
 */
fun createLxScriptEngine(
    scriptSource: String,
    httpClient: LxHttpClient = LxHttpClient(),
    crypto: LxCrypto = LxCryptoJvm(),
    requestTimeoutMs: Long = 15_000L,
    onScriptLog: ((level: String, message: String) -> Unit)? = null,
): LxScriptEngine = LxScriptEngine(
    scriptSource = scriptSource,
    crypto = crypto,
    httpClient = httpClient,
    requestTimeoutMs = requestTimeoutMs,
    onScriptLog = onScriptLog,
)
