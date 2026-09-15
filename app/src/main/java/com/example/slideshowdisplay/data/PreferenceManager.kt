package com.example.slideshowdisplay.data

object PreferenceManager {
    private const val PREFS_NAME = "slideshow_prefs"
    private const val KEY_ZIP = "zip_code"

    fun saveConfig(context: android.content.Context, zip: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (!zip.isNullOrEmpty()) putString(KEY_ZIP, zip)
            apply()
        }
    }

    fun getZip(context: android.content.Context): String? {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getString(KEY_ZIP, null)
    }
}
