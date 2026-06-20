package com.movie.app.best.data.repository

import com.movie.app.best.data.model.Zee5TokensResponse
import com.movie.app.best.data.remote.Zee5ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Zee5TokenRepository @Inject constructor(
    private val zee5Api: Zee5ApiService
) {
    @Volatile
    private var cachedTokens: Zee5TokensResponse? = null
    private var tokenExpiry: Long = 0
    private val TTL = 82800000L // 23hr

    suspend fun prefetchTokens() {
        try { refreshTokens() } catch (_: Exception) {}
    }

    private suspend fun refreshTokens(): Zee5TokensResponse? {
        return try {
            val resp = zee5Api.getTokens()
            cachedTokens = resp
            tokenExpiry = System.currentTimeMillis() + TTL
            resp
        } catch (_: Exception) { null }
    }

    suspend fun getTokens(): Zee5TokensResponse? {
        cachedTokens?.let { tokens ->
            if (System.currentTimeMillis() < tokenExpiry) return tokens
        }
        return refreshTokens()
    }

    fun buildAuthHeaders(tokens: Zee5TokensResponse): Map<String, String> {
        return mapOf(
            "X-ACCESS-TOKEN" to tokens.platformToken,
            "X-Z5-Guest-Token" to tokens.guestToken,
            "x-z5-device-id" to tokens.deviceId,
            "X-Z5-Appversion" to tokens.appVersion,
            "X-Z5-Appversionnumber" to "203230889",
            "X-Z5-AppPlatform" to "androidtv",
            "User-Agent" to "com.graymatrix.did;androidtv;${tokens.appVersion}(203230889);Android(14);Google(Pixel 7)",
            "G_ID" to tokens.gId,
            "X-Forwarded-For" to tokens.xffIp
        )
    }
}
