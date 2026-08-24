package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 凭据仓库测试：用 JVM AES-GCM 引擎替代 AndroidKeyStore（Keystore 在 JVM 单测不可用），
 * 验证存取/清除/密文不落明文语义；AndroidKeystoreCryptoEngine 的加解密路径在真机回归覆盖。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidKeyStoreCredentialsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var repository: AndroidKeyStoreCredentialsRepository
    private lateinit var dataStoreFile: File

    /** createRepository() 创建的 DataStore 引用，供密文校验用例读取内存快照 */
    private var dataStore: DataStore<Preferences>? = null

    private val jvmCryptoEngine = AesGcmCryptoEngine(
        SecretKeySpec(KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().encoded, "AES"),
    )

    @Before
    fun setUp() {
        dataStoreFile = File(tmpFolder.root, "test_credentials.preferences_pb")
    }

    private fun TestScope.createRepository(): AndroidKeyStoreCredentialsRepository {
        val ds = PreferenceDataStoreFactory.create(scope = this, produceFile = { dataStoreFile })
        dataStore = ds
        return AndroidKeyStoreCredentialsRepository(
            dataStore = ds,
            cryptoEngine = jvmCryptoEngine,
        )
    }

    @Test
    fun 保存后可读取原文() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        repository.savePassword("source-1", "p@ssw0rd 中文")
        assertEquals("p@ssw0rd 中文", repository.getPassword("source-1"))
    }

    @Test
    fun 未保存的音源返回null() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        assertNull(repository.getPassword("missing"))
    }

    @Test
    fun 清除后返回null且不影响其他音源() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        repository.savePassword("a", "pw-a")
        repository.savePassword("b", "pw-b")
        repository.clearPassword("a")
        assertNull(repository.getPassword("a"))
        assertEquals("pw-b", repository.getPassword("b"))
    }

    @Test
    fun 密文不含明文密码() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        val secret = "super-secret-password"
        repository.savePassword("x", secret)
        val raw = dataStoreFile.takeIf { it.exists() }?.readBytes()?.decodeToString().orEmpty()
        // 文件可能尚未 flush，仅在文件存在时校验
        if (raw.isNotEmpty()) {
            assertEquals(false, raw.contains(secret))
        }
        // 无论文件状态：DataStore 内存快照中的值必须是密文（base64），不是明文
        val stored = checkNotNull(dataStore).data.first().asMap().values.firstOrNull()?.toString()
            ?: error("应存在已存储凭据")
        assertEquals(false, stored.contains(secret))
    }

    @Test(expected = IllegalArgumentException::class)
    fun 空密码拒绝写入() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        repository.savePassword("y", "")
    }
}
