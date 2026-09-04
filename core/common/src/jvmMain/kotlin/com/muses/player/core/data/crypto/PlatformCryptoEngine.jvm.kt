package com.muses.player.core.data.crypto

import com.muses.player.core.data.platform.PlatformDirs
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * S1 jvmMain actual：Windows DPAPI 优先，失败回退本地 AES-256-GCM 文件密钥。
 *
 * - 首选 DPAPI（Crypt32Util.cryptProtectData/cryptUnprotectData，用户级绑定，
 *   重装系统/换用户即失效——与 AndroidKeyStore 换设备失效语义对齐）；
 * - DPAPI 不可用（非 Windows / 调用抛错）时回退：`appDataDir/dpapi-fallback.key`
 *   存 32 字节随机密钥，AES-256-GCM 加解密；
 * - 密文统一格式：一字节版本前缀（0x01=DPAPI，0x02=回退AES），解密时按前缀分流；
 *   无前缀老格式（纯安卓 `iv || ciphertext`）按回退 AES 尝试解密。
 *
 * JNA 只进 jvmMain（core/common 的 jvmMain.dependencies，见 build.gradle.kts），
 * 不进 commonMain；commonMain 保持平台无关。
 */
actual object PlatformCryptoEngine {

    private const val VERSION_DPAPI: Byte = 0x01
    private const val VERSION_FALLBACK_AES: Byte = 0x02
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val FALLBACK_KEY_NAME = "dpapi-fallback.key"

    private val isWindows: Boolean =
        System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true

    actual fun encrypt(plain: ByteArray): ByteArray {
        if (isWindows) {
            val dpapi = runCatching { Crypt32Util.cryptProtectData(plain) }.getOrNull()
            if (dpapi != null) return byteArrayOf(VERSION_DPAPI) + dpapi
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, fallbackKey())
        return byteArrayOf(VERSION_FALLBACK_AES) + cipher.iv + cipher.doFinal(plain)
    }

    actual fun decrypt(blob: ByteArray): ByteArray {
        require(blob.isNotEmpty()) { "密文长度非法" }
        return when (blob[0]) {
            VERSION_DPAPI -> {
                check(isWindows) { "DPAPI 密文需在 Windows 下解密" }
                Crypt32Util.cryptUnprotectData(blob.copyOfRange(1, blob.size))
            }
            VERSION_FALLBACK_AES -> decryptFallback(blob.copyOfRange(1, blob.size))
            else -> decryptFallback(blob) // 无前缀老格式：按回退 AES 尝试
        }
    }

    private fun decryptFallback(blob: ByteArray): ByteArray {
        require(blob.size > IV_LENGTH_BYTES) { "密文长度非法" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, fallbackKey(), GCMParameterSpec(TAG_LENGTH_BITS, blob, 0, IV_LENGTH_BYTES))
        return cipher.doFinal(blob, IV_LENGTH_BYTES, blob.size - IV_LENGTH_BYTES)
    }

    @Volatile
    private var cachedKey: SecretKeySpec? = null

    private fun fallbackKey(): SecretKeySpec {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }
            val keyFile = File(PlatformDirs.appDataDir(), FALLBACK_KEY_NAME)
            val bytes = if (keyFile.exists() && keyFile.length() == 32L) {
                keyFile.readBytes()
            } else {
                val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
                keyFile.parentFile?.mkdirs()
                keyFile.writeBytes(fresh)
                fresh
            }
            return SecretKeySpec(bytes, "AES").also { cachedKey = it }
        }
    }
}
