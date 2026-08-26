package com.muses.player.core.data.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** WebDAV 等敏感凭据存取（密码 Keystore 加密后落盘，明文只在调用方短生命周期内存在） */
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

/**
 * AndroidKeyStore AES-256-GCM 加密引擎。
 * 密钥不可导出；密文格式 base64( iv[12] || ciphertext+tag )。
 */
@Singleton
class AndroidKeystoreCryptoEngine @Inject constructor() : CryptoEngine {

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_LENGTH_BYTES) { "密文长度非法" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, obtainKey(), GCMParameterSpec(TAG_LENGTH_BITS, blob, 0, IV_LENGTH_BYTES))
        return cipher.doFinal(blob, IV_LENGTH_BYTES, blob.size - IV_LENGTH_BYTES)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "muses_credentials_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}

/** 供测试注入的 JVM AES-GCM 实现（密钥由外部给定，非 Keystore） */
class AesGcmCryptoEngine(private val key: javax.crypto.spec.SecretKeySpec) : CryptoEngine {
    private fun newCipher(): Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = newCipher()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > 12) { "密文长度非法" }
        val cipher = newCipher()
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 0, 12))
        return cipher.doFinal(blob, 12, blob.size - 12)
    }
}

/**
 * 凭据仓库实现：DataStore 中存 base64 加密串，加解密委托 [CryptoEngine]。
 */
@Singleton
class AndroidKeyStoreCredentialsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoEngine: CryptoEngine,
) : CredentialsRepository {

    override suspend fun savePassword(sourceId: String, password: String) {
        require(password.isNotEmpty()) { "密码不能为空" }
        val encrypted = cryptoEngine.encrypt(password.toByteArray(Charsets.UTF_8))
        val encoded = Base64.getEncoder().encodeToString(encrypted)
        dataStore.edit { prefs -> prefs[keyFor(sourceId)] = encoded }
    }

    override suspend fun getPassword(sourceId: String): String? {
        val encoded = dataStore.data.first()[keyFor(sourceId)] ?: return null
        return runCatching {
            String(cryptoEngine.decrypt(Base64.getDecoder().decode(encoded)), Charsets.UTF_8)
        }.getOrNull()
    }

    override suspend fun clearPassword(sourceId: String) {
        dataStore.edit { prefs -> prefs.remove(keyFor(sourceId)) }
    }

    private fun keyFor(sourceId: String) = stringPreferencesKey("credential.$sourceId")
}
