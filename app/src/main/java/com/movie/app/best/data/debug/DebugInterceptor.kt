package com.movie.app.best.data.debug

import okhttp3.Interceptor
import okhttp3.Response
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.io.IOException

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

            if (!response.isSuccessful && response.code >= 500) {
                NetworkMonitor.reportFailure()
            }

            response
        } catch (e: Exception) {
            NetworkLogger.logError(url, "${e.javaClass.simpleName}: ${e.message}")

            when (e) {
                is UnknownHostException,
                is SocketTimeoutException,
                is ConnectException -> NetworkMonitor.reportFailure()
                is IOException -> {
                    if (e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("connection", ignoreCase = true) == true ||
                        e.message?.contains("host", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ||
                        e.message?.contains("resolve", ignoreCase = true) == true) {
                        NetworkMonitor.reportFailure()
                    }
                }
            }

            throw e
        }
    }
}
