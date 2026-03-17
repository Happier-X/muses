package com.muses.api

import com.muses.data.model.*
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): User

    @GET("songs")
    suspend fun getSongs(): List<SongDto>

    @GET("artists")
    suspend fun getArtists(): List<ArtistDto>

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("playlists")
    suspend fun getPlaylists(): List<PlaylistDto>

    @POST("playlists")
    suspend fun createPlaylist(@Body name: String): PlaylistDto

    @GET("favorites")
    suspend fun getFavorites(): List<FavoriteDto>

    @POST("favorites/{songId}")
    suspend fun addFavorite(@Path("songId") songId: Int)

    @DELETE("favorites/{songId}")
    suspend fun removeFavorite(@Path("songId") songId: Int)
}

data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(val username: String, val password: String)
data class AuthResponse(val token: String, val user: User)
