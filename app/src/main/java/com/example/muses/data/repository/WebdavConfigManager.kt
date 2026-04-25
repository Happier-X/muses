package com.example.muses.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.muses.data.model.WebdavConfig

/**
 * Manages WebDAV server configuration via SharedPreferences.
 * Single-server MVP — stores one set of credentials.
 */
object WebdavConfigManager {
    private const val PREFS_NAME = "webdav_config"
    private const val KEY_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    fun save(context: Context, config: WebdavConfig) {
        getPrefs(context).edit()
            .putString(KEY_URL, config.serverUrl)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .apply()
    }

    fun load(context: Context): WebdavConfig? {
        val prefs = getPrefs(context)
        val url = prefs.getString(KEY_URL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val password = prefs.getString(KEY_PASSWORD, "") ?: ""
        return WebdavConfig(url, username, password)
    }

    fun getBasicAuthHeader(context: Context): String? {
        val config = load(context) ?: return null
        val credentials = "${config.username}:${config.password}"
        return "Basic " + Base64.encodeToString(
            credentials.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
