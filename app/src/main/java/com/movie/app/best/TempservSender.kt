package com.movie.app.best

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class TempservSender : ReportSender {

    override fun send(context: Context, errorContent: CrashReportData) {
        val prefs = CrashPasteManager.getPrefs(context)
        val slug = CrashPasteManager.getSlug(prefs)
        val token = CrashPasteManager.getToken(prefs)
        val pasteUrl = CrashPasteManager.getUrl(prefs)

        val reportText = buildString {
            appendLine("=== CRASH REPORT ${java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())} ===")
            appendLine()
            errorContent.toMap().forEach { (k, v) ->
                appendLine("$k = $v")
            }
        }

        try {
            val slug = CrashPasteManager.getSlug(prefs) ?: return
            val token = CrashPasteManager.getToken(prefs) ?: return

            val writeJson = JSONObject()
            writeJson.put("text", reportText)
            writeJson.put("action", "clean&add")

            val conn = URL("https://tempserv.cmdnode.xyz/api/paste/$slug")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Access-Token", token)
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.write(writeJson.toString().toByteArray())
            conn.outputStream.flush()
            conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val url = CrashPasteManager.getUrl(prefs) ?: "https://tempserv.cmdnode.xyz/paste/$slug"
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Crash URL", url))

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Crash report: $url", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            val pending = prefs.getStringSet("pending", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            pending.add(reportText)
            prefs.edit().putStringSet("pending", pending).apply()

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Crash saved offline", Toast.LENGTH_LONG).show()
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
