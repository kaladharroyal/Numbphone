package com.minimalphone

import android.app.Application
import com.minimalphone.core.common.MinimalLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MinimalPhoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MinimalLog.i(TAG, "Minimal Phone Application initialized")
    }

    companion object {
        private const val TAG = "AppInit"
    }
}
