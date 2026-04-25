package com.example.muses.data.model

import androidx.compose.runtime.Immutable

/**
 * WebDAV server configuration.
 */
@Immutable
data class WebdavConfig(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    /** Returns the normalized base URL with trailing slash. */
    val baseUrl: String
        get() {
            val trimmed = serverUrl.trimEnd('/')
            return "$trimmed/"
        }

    fun isValid(): Boolean {
        return serverUrl.isNotBlank() &&
                (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))
    }
}
