package com.movie.app.best.data.debug

import okhttp3.Interceptor
import okhttp3.Response

class DebugInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        val headers = request.headers.toMap()

        NetworkLogger.logRequest(method, url, headers)

        val startMs = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            val tookMs = System.currentTimeMillis() - startMs

            val respHeaders = response.headers.toMap()
            val bodyPreview = try {
                response.peekBody(2048).string().take(500)
            } catch (_: Exception) { "" }

            NetworkLogger.logResponse(response.code, url, respHeaders, bodyPreview, tookMs)
            response
        } catch (e: Exception) {
            NetworkLogger.logError(url, "${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }
}
