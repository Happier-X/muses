package com.muses.player.core.lxsdk.crypto

import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * [LxCrypto] 的 JVM/Android 实现（jvmShared，一份代码双端共用）。
 *
 * 语义对齐洛雪（Node.js）：
 * - `md5` → 小写 hex；
 * - `aesEncrypt` → 洛雪返回**大写 hex** 字符串（音源签名按此拼接）；
 * - `rsaEncrypt` → 公钥 PEM（支持 PKCS#1 与 SPKI 两种常见形态），输出大写 hex；
 * - `buffer.from(str, 'hex')` 等编码语义对齐 Node Buffer。
 */
open class LxCryptoJvm : LxCrypto {

    override fun md5(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .toHexLower()

    override fun randomBytes(size: Int): String {
        val bytes = ByteArray(size.coerceAtLeast(0))
        SecureRandom().nextBytes(bytes)
        return bytes.toHexLower()
    }

    override fun aesEncrypt(data: ByteArray, mode: String, key: String, iv: String?): String {
        val (algorithm, cipherMode, padding) = parseAesMode(mode)
        val cipher = Cipher.getInstance("$algorithm/$cipherMode/$padding")
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            keySpec,
            if (cipherMode == "ECB") null else IvParameterSpec((iv ?: "").toByteArray(Charsets.UTF_8)),
        )
        // 洛雪 aesEncrypt 返回大写 hex（音源签名按此拼接）
        return cipher.doFinal(data).toHexUpper()
    }

    override fun rsaEncrypt(data: ByteArray, key: String): String {
        val publicKey = parseRsaPublicKey(key)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data).toHexUpper()
    }

    override fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream(data.size * 4)
        val buffer = ByteArray(8192)
        try {
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                } else {
                    out.write(buffer, 0, n)
                }
            }
        } finally {
            inflater.end()
        }
        return out.toByteArray()
    }

    override fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(data.size)
        val buffer = ByteArray(8192)
        try {
            while (!deflater.finished()) {
                val n = deflater.deflate(buffer)
                out.write(buffer, 0, n)
            }
        } finally {
            deflater.end()
        }
        return out.toByteArray()
    }

    override fun bufferFrom(data: String, encoding: String): ByteArray = when (encoding.lowercase()) {
        "hex" -> data.hexToBytes()
        "base64" -> Base64.getDecoder().decode(data)
        "latin1", "binary" -> data.toByteArray(Charsets.ISO_8859_1)
        else -> data.toByteArray(Charsets.UTF_8)
    }

    override fun bufferToString(data: ByteArray, format: String): String = when (format.lowercase()) {
        "hex" -> data.toHexLower()
        "base64" -> Base64.getEncoder().encodeToString(data)
        "latin1", "binary" -> String(data, Charsets.ISO_8859_1)
        else -> String(data, Charsets.UTF_8)
    }

    // ── 内部工具 ──

    /** 解析 `aes-128-cbc` / `aes-256-ecb` 形态 → (AES, CBC, PKCS5Padding) */
    private fun parseAesMode(mode: String): Triple<String, String, String> {
        val normalized = mode.lowercase().removePrefix("aes-")
        // 形如 128-cbc / 256-ecb / 192-cfb
        val cipherMode = normalized.substringAfter('-', "cbc").uppercase()
        val padding = when (cipherMode) {
            "ECB", "CBC" -> "PKCS5Padding"
            else -> "NoPadding"
        }
        return Triple("AES", if (cipherMode == "ECB") "ECB" else cipherMode, padding)
    }

    /** 解析 RSA 公钥 PEM：兼容 SPKI(X.509) 与 PKCS#1 两种常见形态 */
    private fun parseRsaPublicKey(pem: String): java.security.PublicKey {
        val body = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "")
        val der = Base64.getDecoder().decode(body)
        val factory = KeyFactory.getInstance("RSA")
        return runCatching { factory.generatePublic(X509EncodedKeySpec(der)) }
            .getOrElse {
                // PKCS#1：需手工包一层 SPKI 头
                factory.generatePublic(X509EncodedKeySpec(wrapPkcs1ToSpki(der)))
            }
    }

    /** 给 PKCS#1 RSAPublicKey 裸码流套上 SPKI 头（RSA + BIT STRING） */
    private fun wrapPkcs1ToSpki(pkcs1: ByteArray): ByteArray {
        val spkiPrefix = intArrayOf(
            0x30, 0x82, 0x01, 0x22, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86,
            0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0F, 0x00,
        ).map { it.toByte() }.toByteArray()
        return spkiPrefix + pkcs1
    }

    private fun ByteArray.toHexLower(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun ByteArray.toHexUpper(): String = toHexLower().uppercase()

    private fun String.hexToBytes(): ByteArray {
        val clean = trim()
        if (clean.length % 2 != 0) return toByteArray(Charsets.UTF_8)
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toIntOrNull(16)?.toByte() ?: 0
        }
    }
}
