package com.autobox.autoboxlauncher

import android.app.Application
import com.autobox.autoboxlauncher.BuildConfig
import com.tomtom.sdk.common.configuration.buildSdkConfiguration
import com.tomtom.sdk.init.TomTomSdk

class AutoBoxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val sdkConfig = buildSdkConfiguration(
            context = this,
            apiKey = BuildConfig.TOMTOM_API_KEY
        )
        TomTomSdk.initialize(this, sdkConfig)
    }
}
