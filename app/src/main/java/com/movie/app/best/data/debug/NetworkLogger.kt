package com.movie.app.best.data.debug

import java.text.SimpleDateFormat
import java.util.*

object NetworkLogger {
    private val logs = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var enabled = false
    private const val MAX_LOG_SIZE = 500_000

    fun setEnabled(on: Boolean) {
        enabled = on
        if (on && logs.isEmpty()) {
            log("SYSTEM", "Debug mode enabled at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        }
    }

    fun isEnabled(): Boolean = enabled

    fun log(tag: String, message: String) {
        if (!enabled) return
        synchronized(this) {
            val ts = dateFormat.format(Date())
            logs.append("[$ts] [$tag] $message\n")
            if (logs.length > MAX_LOG_SIZE) {
                logs.delete(0, logs.length - MAX_LOG_SIZE / 2)
            }
        }
    }

    fun logRequest(method: String, url: String, headers: Map<String, String>) {
        if (!enabled) return
        log("REQ", "$method $url")
        for ((k, v) in headers) {
            log("REQ_HDR", "  $k: $v")
        }
    }

    fun logResponse(code: Int, url: String, headers: Map<String, String>, bodyPreview: String, tookMs: Long) {
        if (!enabled) return
        log("RESP", "$code $url (${tookMs}ms)")
        for ((k, v) in headers) {
            log("RESP_HDR", "  $k: $v")
        }
        if (bodyPreview.isNotEmpty()) {
            log("BODY", "  ${bodyPreview.take(500)}")
        }
    }

    fun logError(url: String, error: String) {
        if (!enabled) return
        log("ERR", "FAILED $url - $error")
    }

    fun logAction(action: String, detail: String) {
        if (!enabled) return
        log("ACTION", "$action: $detail")
    }

    fun getLogs(): String = synchronized(this) { logs.toString() }

    fun clear() {
        synchronized(this) { logs.clear() }
    }

    fun getLogCount(): Int = synchronized(this) { logs.count { it == '\n' } }
}
