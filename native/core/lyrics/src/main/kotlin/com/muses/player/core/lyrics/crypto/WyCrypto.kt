package com.muses.player.core.lyrics.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 网易云 eapi 参数加密（AES-128-ECB + MD5）。
 *
 * 规格书 = src/features/lyrics/providers/wyCrypto.ts（算法对齐 NeteaseCloudMusicApi / any-listen）。
 * 按 D3 决策用 JVM 平台原语（MessageDigest/Cipher）替代 Web 手写的 MD5/AES 实现，
 * 输出语义一致：MD5 小写 hex；AES-128-ECB PKCS7 填充 → 大写 hex。
 */
object WyCrypto {

    private val EAPI_KEY = "e82ckenh8dichen8".toByteArray(Charsets.UTF_8)

    /** md5(message)：小写 hex（wyCrypto.ts md5） */
    fun md5Hex(message: String): String =
        MessageDigest.getInstance("MD5")
            .digest(message.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    /** AES-128-ECB PKCS7 加密 → 大写 hex（wyCrypto.ts aes128EcbEncryptPkcs7 + toHexUpper） */
    private fun aesEcbEncryptHexUpper(plain: ByteArray, key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(plain)
            .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            .uppercase()
    }

    /** 生成 eapi form 字段 params（大写 hex；wyCrypto.ts buildEapiParams） */
    fun buildEapiParams(apiPath: String, payloadJson: String): String {
        val message = "nobody${apiPath}use${payloadJson}md5forencrypt"
        val digest = md5Hex(message)
        val data = "$apiPath-36cd479b6b5-$payloadJson-36cd479b6b5-$digest"
        return aesEcbEncryptHexUpper(data.toByteArray(Charsets.UTF_8), EAPI_KEY)
    }
}
