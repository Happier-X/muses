package com.muses.player.core.lxsdk.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [LxCryptoJvm] 单测：对齐 Node.js（洛雪宿主）的语义 */
class LxCryptoJvmTest {

    private val crypto = LxCryptoJvm()

    @Test
    fun `md5 返回小写十六进制`() {
        // 标准测试向量
        assertEquals("900150983cd24fb0d6963f7d28e17f72", crypto.md5("abc"))
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", crypto.md5(""))
        assertEquals("5d41402abc4b2a76b9719d911017c592", crypto.md5("hello"))
    }

    @Test
    fun `randomBytes 返回指定长度的十六进制串`() {
        val result = crypto.randomBytes(16)
        // 每字节 2 个十六进制字符
        assertEquals(32, result.length)
        assertTrue(result.all { it.isDigit() || it in 'a'..'f' })
        // 两次调用应不同（随机性冒烟）
        assertTrue(crypto.randomBytes(16) != result)
    }

    @Test
    fun `aes-128-ecb 加密返回大写十六进制`() {
        // 洛雪 aesEncrypt 语义：PKCS5/7 填充，输出大写 hex
        // 可验证向量（JCE 实测）：全零 key + 15 字节全零明文 → PKCS5 补 1 字节，
        // 等价于对全零块做 ECB 加密 = 58e2fccefa7e3061367f1d57a4e7455a
        val plain = ByteArray(15)
        val zeroKey = "\u0000".repeat(16)
        val result = crypto.aesEncrypt(plain, "aes-128-ecb", zeroKey, null)
        assertEquals("58E2FCCEFA7E3061367F1D57A4E7455A", result)
    }

    @Test
    fun `aes 输出为大写十六进制且长度符合块对齐`() {
        val key = "0123456789abcdef"
        val iv = "abcdef0123456789"
        // 5 字节明文 PKCS5 填充至 16 字节 → 密文 32 个十六进制字符
        val result = crypto.aesEncrypt("hello".toByteArray(), "aes-128-cbc", key, iv)
        assertEquals(32, result.length)
        assertTrue(result.all { it.isDigit() || it in 'A'..'F' })
        // 16 字节明文 → 再加一个填充块 → 32 字节密文
        val exact = crypto.aesEncrypt(ByteArray(16), "aes-128-cbc", key, iv)
        assertEquals(64, exact.length)
    }

    @Test
    fun `bufferFrom 支持 hex 解码`() {
        val bytes = crypto.bufferFrom("48656c6c6f", "hex")
        assertContentEquals("Hello".toByteArray(), bytes)
    }

    @Test
    fun `bufferFrom 支持 base64 解码`() {
        val bytes = crypto.bufferFrom("SGVsbG8=", "base64")
        assertContentEquals("Hello".toByteArray(), bytes)
    }

    @Test
    fun `bufferToString 支持 hex 与 base64 编码`() {
        val hello = "Hello".toByteArray()
        assertEquals("48656c6c6f", crypto.bufferToString(hello, "hex"))
        assertEquals("SGVsbG8=", crypto.bufferToString(hello, "base64"))
        assertEquals("Hello", crypto.bufferToString(hello, "utf8"))
    }

    @Test
    fun `hex 与 base64 roundtrip 一致`() {
        val original = "洛雪自定义音源测试".toByteArray()
        val hex = crypto.bufferToString(original, "hex")
        assertContentEquals(original, crypto.bufferFrom(hex, "hex"))

        val base64 = crypto.bufferToString(original, "base64")
        assertContentEquals(original, crypto.bufferFrom(base64, "base64"))
    }

    @Test
    fun `deflate 与 inflate 互为逆运算`() {
        val text = "测".repeat(500)
        val original = text.toByteArray()
        val compressed = crypto.deflate(original)
        assertTrue(compressed.isNotEmpty())
        assertContentEquals(original, crypto.inflate(compressed))
    }
}
