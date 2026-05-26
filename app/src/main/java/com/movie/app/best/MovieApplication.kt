package com.movie.app.best

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.acra.config.httpSender
import org.acra.config.toast
import org.acra.config.dialog
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

@HiltAndroidApp
class MovieApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = org.acra.data.StringFormat.KEY_VALUE_LIST

            toast {
                text = "Oops! Crash ho gaya 😵 Report bhej raha hoon..."
                length = android.widget.Toast.LENGTH_LONG
            }

            dialog {
                title = "Crash Report"
                text = "App crash ho gaya! Kya aap crash report developer ko bhejna chahte ho?"
                commentPrompt = "Kya kar rahe the jab crash hua? (optional)"
                positiveButtonText = "Bhejo"
                negativeButtonText = "Cancel"
                resTheme = android.R.style.Theme_DeviceDefault_Light_Dialog
            }

            httpSender {
                uri = "https://collector.tracepot.com/e6f5c0d0"
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}
