package com.movie.app.best.data.debug

import okhttp3.Interceptor
import okhttp3.Response

class Zee5False404Interceptor : Interceptor {
    private val MEDIA_TYPES = setOf("video/", "audio/", "application/vnd.apple.mpegurl", "application/x-mpegurl", "application/octet-stream")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 404 && request.url.host.contains("zee5vod.akamaized.net")) {
            val contentType = response.header("Content-Type") ?: ""
            if (MEDIA_TYPES.any { contentType.startsWith(it, ignoreCase = true) }) {
                return response.newBuilder()
                    .code(200)
                    .message("OK")
                    .build()
            }
        }

        return response
    }
}
