package com.muses.player.core.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 更新资产解析测试：GitHub releases/latest 响应 → 按产物命名约定挑出的安装包。
 * 纯函数 [parseRelease] 单测，不触网。
 */
class ReleaseInfoTest {

    private fun releaseJson(
        tag: String = "v0.9.6",
        htmlUrl: String = "https://github.com/Happier-X/muses/releases/tag/v0.9.6",
        body: String = "### 更新内容\\n- 应用内更新",
        assets: String = """
            {"name":"muses-v0.9.6.apk","browser_download_url":"https://example.com/a.apk","size":123},
            {"name":"muses-v0.9.6-mi.apk","browser_download_url":"https://example.com/a-mi.apk","size":456},
            {"name":"Muses-v0.9.6.msi","browser_download_url":"https://example.com/Muses-v0.9.6.msi","size":45678901}
        """.trimIndent(),
    ): String = """
        {
          "tag_name":"$tag",
          "html_url":"$htmlUrl",
          "body":"$body",
          "assets":[$assets]
        }
    """.trimIndent()

    /** 与两端实际命名约定一致（见 release.yml）：桌面 MSI / 安卓主包 / 安卓 MIUI 定制包 */
    private val msi: (String) -> String = { tag -> "Muses-$tag.msi" }
    private val mainApk: (String) -> String = { tag -> "muses-$tag.apk" }
    private val miuiApk: (String) -> String = { tag -> "muses-$tag-mi.apk" }

    @Test
    fun 桌面取MSI资产() {
        val info = parseRelease(releaseJson(), msi)
        assertEquals("v0.9.6", info?.tag)
        assertEquals("https://github.com/Happier-X/muses/releases/tag/v0.9.6", info?.htmlUrl)
        assertEquals("https://example.com/Muses-v0.9.6.msi", info?.url)
        assertEquals("Muses-v0.9.6.msi", info?.name)
        assertEquals(45678901L, info?.sizeBytes)
        assertEquals("### 更新内容\n- 应用内更新", info?.notes)
    }

    @Test
    fun 安卓主包与MIUI包按命名约定区分() {
        assertEquals("muses-v0.9.6.apk", parseRelease(releaseJson(), mainApk)?.name)
        assertEquals("muses-v0.9.6-mi.apk", parseRelease(releaseJson(), miuiApk)?.name)
    }

    @Test
    fun 无匹配资产返回null() {
        val apkOnly = """{"name":"muses-v0.9.6.apk","browser_download_url":"https://example.com/a.apk","size":1}"""
        assertNull(parseRelease(releaseJson(assets = apkOnly), msi))
    }

    @Test
    fun 缺tag或地址返回null() {
        assertNull(parseRelease(releaseJson(tag = ""), msi))
        assertNull(parseRelease("""{"tag_name":"v0.9.6","assets":[]}""", msi))
    }

    @Test
    fun 异常正文返回null不抛() {
        assertNull(parseRelease("not json", msi))
        assertNull(parseRelease("{}", msi))
    }
}
