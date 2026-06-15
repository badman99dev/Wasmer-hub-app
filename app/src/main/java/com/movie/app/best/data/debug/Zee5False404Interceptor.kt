package com.movie.app.best.data.debug

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class Zee5False404Interceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url.toString()

        if (url.contains("zee5vod.akamaized.net") && response.code == 404) {
            val body = response.body
            val contentType = body?.contentType()
            val bytes = body?.bytes()

            if (bytes != null && bytes.isNotEmpty()) {
                val newBody = bytes.toResponseBody(contentType)
                return response.newBuilder()
                    .code(200)
                    .message("OK")
                    .body(newBody)
                    .build()
            }
        }

        return response
    }
}
