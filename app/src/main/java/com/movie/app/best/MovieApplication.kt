package com.movie.app.best

import android.app.Application
import com.movie.app.best.data.settings.VideoQualitySettings
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.toast
import org.acra.config.dialog
import org.acra.ktx.initAcra
import org.acra.data.StringFormat

@HiltAndroidApp
class MovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        VideoQualitySettings.initCache(this)

        Thread { CrashPasteManager.ensurePasteExists(this) }.start()

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST

            toast {
                text = "Crash report Tempserv pe bhej raha hoon... 📤"
                length = android.widget.Toast.LENGTH_LONG
            }

            dialog {
                title = "Crash Report"
                text = "App crash ho gaya! Report Tempserv pe upload ho raha hai.\n\nLink copy karke developer ko bhej do."
                commentPrompt = "Kya kar rahe the jab crash hua? (optional)"
                positiveButtonText = "OK"
                negativeButtonText = "Close"
                resTheme = android.R.style.Theme_DeviceDefault_Light_Dialog
            }
        }
    }
}
