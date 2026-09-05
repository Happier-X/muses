package com.muses.player.core.data.repository

/**
 * WebDAV 等敏感凭据存取（密码 Keystore 加密后落盘，明文只在调用方短生命周期内存在）。
 * 安卓侧实现为 [AndroidKeyStoreCredentialsRepository]（core:data，Keystore 引擎绑定）。
 */
interface CredentialsRepository {
    suspend fun savePassword(sourceId: String, password: String)

    /** 返回解密后的密码副本；未存储返回 null。禁止写入日志/状态流/持久化数据。 */
    suspend fun getPassword(sourceId: String): String?

    suspend fun clearPassword(sourceId: String)
}

/** AES-256-GCM 加密引擎抽象：Android 侧绑定 AndroidKeystoreCryptoEngine，测试可注入 JVM 实现 */
interface CryptoEngine {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}
