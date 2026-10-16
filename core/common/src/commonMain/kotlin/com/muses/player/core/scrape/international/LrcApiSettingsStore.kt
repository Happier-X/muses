package com.muses.player.core.scrape.international

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import io.ktor.http.Url
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val DEFAULT_LRC_API_URL = "https://api.lrc.cx"

/** 地址与其他应用设置共用 DataStore；公开服务默认可用，也可接无鉴权的自托管服务。 */
class LrcApiSettingsStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("scrape_lrcapi_base_url")
    val baseUrl = dataStore.data.map { it[key] ?: DEFAULT_LRC_API_URL }
    suspend fun readBaseUrl() = baseUrl.first()
    suspend fun setBaseUrl(value: String) {
        val normalized = value.trim().ifBlank { DEFAULT_LRC_API_URL }.trimEnd('/')
        val url = Url(normalized)
        require(url.protocol.name in setOf("http", "https") && url.host.isNotBlank() &&
            url.user.isNullOrEmpty() && url.password.isNullOrEmpty() && url.parameters.isEmpty() && url.fragment.isEmpty()) {
            "请填写不含账号、密码和查询参数的 HTTP 或 HTTPS 服务地址"
        }
        dataStore.edit { it[key] = normalized }
    }
}
