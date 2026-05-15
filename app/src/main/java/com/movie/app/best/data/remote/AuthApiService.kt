package com.movie.app.best.data.remote

import com.movie.app.best.data.model.AuthResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/verify")
    suspend fun verifyEmail(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authHeader: String
    ): AuthResponse

    @POST("auth/firebase-sync")
    suspend fun firebaseSync(
        @Body request: Map<String, @JvmSuppressWildcards String>
    ): AuthResponse

    @GET("auth/profile")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String
    ): AuthResponse
}
