package com.muses.player.core.lxsdk.crypto

/**
 * 洛雪 `lx.utils` 的工具方法端口（KMP expect/actual）。
 *
 * 洛雪规范提供：
 * - `buffer.from` / `buffer.bufToString`（Node Buffer 语义）
 * - `crypto.md5` / `crypto.aesEncrypt` / `crypto.rsaEncrypt` / `crypto.randomBytes`
 * - `zlib.inflate` / `zlib.deflate`
 *
 * 音源脚本大量依赖这些做签名与解码（尤其 tx/wy 系源），故必须真实实现。
 * 各平台经 actual 落到 JCE（JVM/Android）。
 */
interface LxCrypto {
    /** MD5 十六进制小写摘要（Node `crypto.createHash('md5').digest('hex')` 同口径） */
    fun md5(input: String): String

    /** 随机字节的十六进制字符串（长度 = size * 2） */
    fun randomBytes(size: Int): String

    /**
     * AES 加密，返回十六进制字符串。
     * 对齐洛雪 `aesEncrypt(buffer, mode, key, iv)`；[mode] 形如 `aes-128-cbc`、`aes-256-ecb`。
     */
    fun aesEncrypt(data: ByteArray, mode: String, key: String, iv: String?): String

    /**
     * RSA 加密（公钥 PEM），返回十六进制字符串。
     * 对齐洛雪 `rsaEncrypt(buffer, key)`。
     */
    fun rsaEncrypt(data: ByteArray, key: String): String

    /** zlib 解压（洛雪 `zlib.inflate`） */
    fun inflate(data: ByteArray): ByteArray

    /** zlib 压缩（洛雪 `zlib.deflate`） */
    fun deflate(data: ByteArray): ByteArray

    /** Buffer.from 语义：按 [encoding] 把字符串/十六进制转为字节 */
    fun bufferFrom(data: String, encoding: String): ByteArray

    /** Buffer.toString 语义：按 [format] 把字节转为字符串 */
    fun bufferToString(data: ByteArray, format: String): String
}
