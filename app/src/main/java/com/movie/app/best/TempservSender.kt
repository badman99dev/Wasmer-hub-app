package com.movie.app.best

import android.content.Context
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import org.acra.data.CrashReportData

class TempservSender : ReportSender {

    override fun send(context: Context, errorContent: CrashReportData) {
        try {
            val reportText = buildString {
                appendLine("=== CRASH REPORT ===")
                appendLine("App: ${context.packageName}")
                appendLine("Version: ${getMeta(context, "APP_VERSION_CODE")}")
                appendLine("Android: ${getMeta(context, "ANDROID_VERSION")}")
                appendLine("Device: ${getMeta(context, "BRAND")} ${getMeta(context, "PHONE_MODEL")}")
                appendLine()
                appendLine("=== STACK TRACE ===")
                appendLine(getMeta(context, "STACK_TRACE"))
                appendLine()
                appendLine("=== FULL LOG ===")
                errorContent.toMap().forEach { (k, v) ->
                    appendLine("$k = $v")
                }
            }

            val json = org.json.JSONObject()
            json.put("expiry", "24hr")
            val connection = java.net.URL("https://tempserv.badman993944.workers.dev/api/paste")
                .openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.outputStream.write(json.toString().toByteArray())
            connection.outputStream.flush()

            val createResponse = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val createJson = org.json.JSONObject(createResponse)
            val slug = createJson.getString("slug")
            val accessToken = createJson.getString("accessToken")

            val writeJson = org.json.JSONObject()
            writeJson.put("text", reportText)
            writeJson.put("action", "clean&add")

            val writeConn = java.net.URL("https://tempserv.badman993944.workers.dev/api/paste/$slug")
                .openConnection() as java.net.HttpURLConnection
            writeConn.requestMethod = "POST"
            writeConn.setRequestProperty("Content-Type", "application/json")
            writeConn.setRequestProperty("X-Access-Token", accessToken)
            writeConn.doOutput = true
            writeConn.connectTimeout = 10000
            writeConn.readTimeout = 10000

            writeConn.outputStream.write(writeJson.toString().toByteArray())
            writeConn.outputStream.flush()
            writeConn.inputStream.bufferedReader().readText()
            writeConn.disconnect()

            val prefs = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
            val reports = prefs.getStringSet("urls", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            reports.add("https://tempserv.badman993944.workers.dev/paste/$slug")
            prefs.edit().putStringSet("urls", reports).apply()

        } catch (_: Exception) {
            val reportText = buildString {
                appendLine("=== CRASH REPORT (OFFLINE) ===")
                errorContent.toMap().forEach { (k, v) ->
                    appendLine("$k = $v")
                }
            }
            val prefs = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
            val pending = prefs.getStringSet("pending", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            pending.add(reportText)
            prefs.edit().putStringSet("pending", pending).apply()
        }
    }

    private fun getMeta(context: Context, key: String): String {
        return try {
            val prefs = context.getSharedPreferences("acra", Context.MODE_PRIVATE)
            prefs.getString(key, "unknown") ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}

class TempservSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return TempservSender()
    }
    override fun enabled(config: CoreConfiguration): Boolean = true
}
