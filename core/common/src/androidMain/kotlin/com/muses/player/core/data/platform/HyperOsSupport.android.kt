package com.muses.player.core.data.platform

import android.os.Build

/**
 * HyperOS（小米澎湃）判定：超级岛/焦点通知适配的总开关前置条件。
 *
 * 无 Context 依赖（Build + SystemProperties 反射），core:media 的岛适配层与
 * feature:shell 的设置行共用。结果进程内缓存，反射只走一次。
 */
object HyperOsSupport {

    @Volatile
    private var cached: Boolean? = null

    /** HyperOS 设备（小米/红米/Poco 跑澎湃 OS）返回 true；MIUI 及其他 OEM 返回 false。 */
    fun isHyperOS(): Boolean {
        cached?.let { return it }
        val result = runCatching {
            val manufacturer = Build.MANUFACTURER.orEmpty()
            val isXiaomiFamily = manufacturer.contains("xiaomi", ignoreCase = true) ||
                manufacturer.contains("redmi", ignoreCase = true) ||
                manufacturer.contains("poco", ignoreCase = true)
            // ro.mi.os.version.name 仅 HyperOS 存在（如 "2.0"/"3.0"），MIUI 为空
            isXiaomiFamily && getSystemProperty("ro.mi.os.version.name").isNotEmpty()
        }.getOrDefault(false)
        cached = result
        return result
    }

    private fun getSystemProperty(key: String): String {
        return runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String
        }.getOrNull().orEmpty()
    }
}
