package com.muses.player.core.data.store

import com.muses.player.core.data.platform.PlatformDirs
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S1 桌面路径测试：DataStore 真实路径 + 平台目录策略（design §5）。
 *
 * - APPDATA/LOCALAPPDATA 存在时走漫游/本地两根；缺失时回退用户主目录（由环境决定，不断言具体根）；
 * - dataStoreFilePath 与 appDataDir 同根、文件名冻结 [DATASTORE_NAME]；
 * - errorLogDir 为 appDataDir 下 error_log；cacheDir 真实存在。
 */
class DataStorePathJvmTest {

    @Test
    fun dataStore落盘appData根且文件名冻结() {
        val path = dataStoreFilePath(DATASTORE_NAME)
        val file = File(path)
        assertEquals(DATASTORE_NAME, file.name)
        assertEquals(File(PlatformDirs.appDataDir(), DATASTORE_NAME).absolutePath, file.absolutePath)
        assertTrue(file.parentFile.exists(), "appDataDir 应已创建")
    }

    @Test
    fun 平台三目录均真实存在且errorLog收敛appData下() {
        val appData = File(PlatformDirs.appDataDir())
        val cache = File(PlatformDirs.cacheDir())
        val errorLog = File(PlatformDirs.errorLogDir())
        assertTrue(appData.isDirectory)
        assertTrue(cache.isDirectory)
        assertTrue(errorLog.isDirectory)
        assertEquals(File(appData, "error_log").absolutePath, errorLog.absolutePath)
    }
}
