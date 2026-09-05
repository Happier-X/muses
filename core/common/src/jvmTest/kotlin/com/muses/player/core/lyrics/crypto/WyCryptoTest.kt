package com.muses.player.core.lyrics.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

/** 规格 = src/features/lyrics/providers/wyCrypto.ts（D3：MD5/AES 用 JVM 平台原语） */
class WyCryptoTest {

    @Test
    fun `MD5 标准测试向量`() {
        // RFC 1321 公认向量
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", WyCrypto.md5Hex(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", WyCrypto.md5Hex("abc"))
        assertEquals("5d41402abc4b2a76b9719d911017c592", WyCrypto.md5Hex("hello"))
    }

    @Test
    fun `eapi params 可用同密钥 ECB 解密还原`() {
        val apiPath = "/api/song/lyric/v1"
        val payload = """{"id":123,"cp":false,"tv":-1,"lv":-1,"rv":-1,"kv":-1,"yv":1,"ytv":1,"yrv":-1}"""
        val params = WyCrypto.buildEapiParams(apiPath, payload)

        // 大写 hex、16 字节倍数
        assertEquals(0, params.length % 32)
        assertEquals(params.uppercase(), params)

        // 解密还原 data = path-36cd479b6b5-payload-digest
        val key = "e82ckenh8dichen8".toByteArray(Charsets.UTF_8)
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(key, "AES"))
        val decrypted = String(
            cipher.doFinal(params.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
            Charsets.UTF_8,
        )
        val expectedPrefix = "$apiPath-36cd479b6b5-$payload-36cd479b6b5-"
        assertTrue(decrypted.startsWith(expectedPrefix))
        // 尾部为 message 的 md5
        assertEquals(WyCrypto.md5Hex("nobody${apiPath}use${payload}md5forencrypt"), decrypted.removePrefix(expectedPrefix))
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
