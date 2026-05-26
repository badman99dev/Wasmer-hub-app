package com.movie.app.best

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import org.acra.data.CrashReportData

class TempservSender : ReportSender {

    companion object {
        private const val PASTE_SLUG = "2e1ty3"
        private const val PASTE_TOKEN = "Obbg0"
        private const val PASTE_URL = "https://tempserv.badman993944.workers.dev/paste/$PASTE_SLUG"
        private const val WRITE_URL = "https://tempserv.badman993944.workers.dev/api/paste/$PASTE_SLUG"
    }

    override fun send(context: Context, errorContent: CrashReportData) {
        try {
            val reportText = buildString {
                appendLine("=== CRASH REPORT ${java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())} ===")
                appendLine()
                errorContent.toMap().forEach { (k, v) ->
                    appendLine("$k = $v")
                }
            }

            val writeJson = org.json.JSONObject()
            writeJson.put("text", reportText)
            writeJson.put("action", "clean&add")

            val writeConn = java.net.URL(WRITE_URL)
                .openConnection() as java.net.HttpURLConnection
            writeConn.requestMethod = "POST"
            writeConn.setRequestProperty("Content-Type", "application/json")
            writeConn.setRequestProperty("X-Access-Token", PASTE_TOKEN)
            writeConn.doOutput = true
            writeConn.connectTimeout = 10000
            writeConn.readTimeout = 10000

            writeConn.outputStream.write(writeJson.toString().toByteArray())
            writeConn.outputStream.flush()
            writeConn.inputStream.bufferedReader().readText()
            writeConn.disconnect()

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Crash URL", PASTE_URL))

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Crash report: $PASTE_URL", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            val reportText = buildString {
                appendLine("=== CRASH REPORT (OFFLINE) ===")
                appendLine("Error uploading: ${e.message}")
                errorContent.toMap().forEach { (k, v) ->
                    appendLine("$k = $v")
                }
            }
            val prefs = context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)
            val pending = prefs.getStringSet("pending", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            pending.add(reportText)
            prefs.edit().putStringSet("pending", pending).apply()

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Crash report saved offline", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class TempservSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return TempservSender()
    }
    override fun enabled(config: CoreConfiguration): Boolean = true
}
