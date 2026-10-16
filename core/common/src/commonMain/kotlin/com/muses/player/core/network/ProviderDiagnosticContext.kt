package com.muses.player.core.network

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** 诊断绕过来源查询缓存，不清空或填充正常审核缓存。 */
object ProviderDiagnosticContext : AbstractCoroutineContextElement(Key) {
    object Key : CoroutineContext.Key<ProviderDiagnosticContext>
}
