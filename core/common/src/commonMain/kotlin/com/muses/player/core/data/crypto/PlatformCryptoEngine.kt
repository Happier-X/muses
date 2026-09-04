package com.muses.player.core.data.crypto

/**
 * 凭据加密引擎的平台供给（S1 数据层桌面接线）。
 *
 * commonMain 只声明接口语义：AES-256-GCM 口径，密文格式 base64(iv[12] || ciphertext+tag)，
 * 与安卓侧 AndroidKeystoreCryptoEngine 保持一致，桌面侧 DPAPI 失败回退文件密钥时沿用同一格式。
 * 安卓侧行为不动：仍走 AndroidKeyStore（见 :core:data CredentialsRepository）。
 */
expect object PlatformCryptoEngine {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}
