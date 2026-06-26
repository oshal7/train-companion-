package com.traincompanion.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores small, sensitive app settings (rail-data API provider choice + key) using an
 * Android-Keystore-backed master key. Nothing here is ever uploaded off-device.
 */
class SecurePrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "train_companion_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getSettings(): AppSettings {
        val providerName = prefs.getString(KEY_PROVIDER, RailProviderType.MOCK.name)
            ?: RailProviderType.MOCK.name
        val provider = runCatching { RailProviderType.valueOf(providerName) }
            .getOrDefault(RailProviderType.MOCK)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        return AppSettings(provider, apiKey)
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_PROVIDER, settings.providerType.name)
            .putString(KEY_API_KEY, settings.apiKey)
            .apply()
    }

    companion object {
        private const val KEY_PROVIDER = "provider_type"
        private const val KEY_API_KEY = "api_key"
    }
}
