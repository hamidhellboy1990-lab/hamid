package com.example.goldoverlay

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "gold_overlay_prefs"
    private const val KEY_API_KEY = "api_key"

    // کلید پیش‌فرض GoldAPI.io — در صورت نیاز از داخل اپ هم قابل تغییر است
    private const val DEFAULT_API_KEY = "goldapi-40d3ff0fd09e30919d8965f89b8dbca0-io"

    fun saveApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }
}
