package com.muses.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muses.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muses_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService
) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    suspend fun login(username: String, password: String): AuthResponse {
        val response = api.login(LoginRequest(username, password))
        saveToken(response.token)
        return response
    }

    suspend fun register(username: String, password: String): AuthResponse {
        val response = api.register(RegisterRequest(username, password))
        saveToken(response.token)
        return response
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[TOKEN_KEY] }.first()
    }

    suspend fun logout() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    private suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }
}

@Singleton
class MusicRepository @Inject constructor(private val api: ApiService) {
    suspend fun getSongs() = api.getSongs()
    suspend fun getArtists() = api.getArtists()
    suspend fun getAlbums() = api.getAlbums()
    suspend fun getPlaylists() = api.getPlaylists()
    suspend fun createPlaylist(name: String) = api.createPlaylist(name)
    suspend fun getFavorites() = api.getFavorites()
    suspend fun addFavorite(songId: Int) = api.addFavorite(songId)
    suspend fun removeFavorite(songId: Int) = api.removeFavorite(songId)
}
