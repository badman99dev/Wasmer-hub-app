package com.movie.app.best.data.repository

import com.movie.app.best.data.model.MeiliSearchRequest
import com.movie.app.best.data.model.MeiliSearchResponse
import com.movie.app.best.data.remote.MeiliSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeiliSearchRepository @Inject constructor(
    private val meiliService: MeiliSearchService
) {
    @Volatile
    private var cachedKey: String? = null
    private var keyExpiry: Long = 0
    private val KEY_TTL = 3600000L

    private val plainClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val KEY_URL = "https://meilisearch.badman993944.workers.dev/"
    private val PING_URL = "https://meilisearch-rs25.onrender.com/"

    suspend fun pingAndPrefetchKey() {
        coroutineScope {
            launch(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(PING_URL).get().build()
                    plainClient.newCall(req).execute().close()
                } catch (_: Exception) {}
            }
            launch(Dispatchers.IO) {
                try { refreshKey() } catch (_: Exception) {}
            }
        }
    }

    private suspend fun refreshKey(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(KEY_URL).get().build()
                val resp = plainClient.newCall(req).execute()
                val body = resp.body?.string()
                resp.close()
                val key = JSONObject(body ?: "").optString("key", "")
                if (key.isNotEmpty()) {
                    cachedKey = key
                    keyExpiry = System.currentTimeMillis() + KEY_TTL
                    key
                } else null
            } catch (_: Exception) { null }
        }
    }

    suspend fun getKey(): String? {
        cachedKey?.let { key ->
            if (System.currentTimeMillis() < keyExpiry) return key
        }
        return refreshKey()
    }

    suspend fun search(query: String, limit: Int = 20, offset: Int = 0): MeiliSearchResponse {
        val key = getKey() ?: return MeiliSearchResponse()
        val body = MeiliSearchRequest(q = query, limit = limit, offset = offset)
        return try {
            meiliService.search(auth = "Bearer $key", body = body)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                cachedKey = null
                val freshKey = refreshKey()
                if (freshKey != null) {
                    try {
                        meiliService.search(auth = "Bearer $freshKey", body = body)
                    } catch (_: Exception) { MeiliSearchResponse() }
                } else MeiliSearchResponse()
            } else MeiliSearchResponse()
        } catch (_: Exception) { MeiliSearchResponse() }
    }

    suspend fun suggest(query: String, limit: Int = 8): MeiliSearchResponse {
        return search(query, limit = limit, offset = 0)
    }
}
