package com.movie.app.best

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CrashPasteManager {

    private const val PREFS_NAME = "crash_reports"
    private const val KEY_SLUG = "paste_slug"
    private const val KEY_TOKEN = "paste_token"
    private const val KEY_URL = "paste_url"
    private const val BASE = "https://tempserv.cmdnode.xyz"

    fun ensurePasteExists(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_SLUG)) return

        try {
            val json = JSONObject().put("expiry", "24hr")
            val conn = URL("$BASE/api/paste").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.write(json.toString().toByteArray())
            conn.outputStream.flush()
            val resp = JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()

            val slug = resp.getString("slug")
            val token = resp.getString("accessToken")
            val url = resp.getString("url")

            prefs.edit()
                .putString(KEY_SLUG, slug)
                .putString(KEY_TOKEN, token)
                .putString(KEY_URL, url)
                .apply()
        } catch (_: Exception) {}
    }

    fun getSlug(prefs: android.content.SharedPreferences?): String? = prefs?.getString(KEY_SLUG, null)
    fun getToken(prefs: android.content.SharedPreferences?): String? = prefs?.getString(KEY_TOKEN, null)
    fun getUrl(prefs: android.content.SharedPreferences?): String? = prefs?.getString(KEY_URL, null)
    fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
