package com.autobox.autoboxlauncher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val THEME = stringPreferencesKey("theme")
    val UNIT_SYSTEM = stringPreferencesKey("unit_system")
    val SIGN_RECOGNITION_ENABLED = booleanPreferencesKey("sign_recognition_enabled")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val MEDIA_APP_PACKAGE = stringPreferencesKey("media_app_package")
}
