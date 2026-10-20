package com.fuwanime.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val amoledBlack: Boolean = false,
    /** Preferred stream height (e.g. 720); the closest available source is picked. */
    val preferredQuality: Int = 720,
    val autoPlayNext: Boolean = true,
    val autoPip: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    /** Downloads default lower than streaming to save storage. */
    val downloadQuality: Int = 480,
    val downloadWifiOnly: Boolean = false,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val amoledBlack = booleanPreferencesKey("amoled_black")
        val preferredQuality = intPreferencesKey("preferred_quality")
        val autoPlayNext = booleanPreferencesKey("auto_play_next")
        val autoPip = booleanPreferencesKey("auto_pip")
        val doubleTapSeekSeconds = intPreferencesKey("double_tap_seek_seconds")
        val downloadQuality = intPreferencesKey("download_quality")
        val downloadWifiOnly = booleanPreferencesKey("download_wifi_only")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
            amoledBlack = this[Keys.amoledBlack] ?: d.amoledBlack,
            preferredQuality = this[Keys.preferredQuality] ?: d.preferredQuality,
            autoPlayNext = this[Keys.autoPlayNext] ?: d.autoPlayNext,
            autoPip = this[Keys.autoPip] ?: d.autoPip,
            doubleTapSeekSeconds = this[Keys.doubleTapSeekSeconds] ?: d.doubleTapSeekSeconds,
            downloadQuality = this[Keys.downloadQuality] ?: d.downloadQuality,
            downloadWifiOnly = this[Keys.downloadWifiOnly] ?: d.downloadWifiOnly,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = enabled }
    suspend fun setAmoledBlack(enabled: Boolean) = context.dataStore.edit { it[Keys.amoledBlack] = enabled }
    suspend fun setPreferredQuality(height: Int) = context.dataStore.edit { it[Keys.preferredQuality] = height }
    suspend fun setAutoPlayNext(enabled: Boolean) = context.dataStore.edit { it[Keys.autoPlayNext] = enabled }
    suspend fun setAutoPip(enabled: Boolean) = context.dataStore.edit { it[Keys.autoPip] = enabled }
    suspend fun setDoubleTapSeekSeconds(seconds: Int) = context.dataStore.edit { it[Keys.doubleTapSeekSeconds] = seconds }
    suspend fun setDownloadQuality(height: Int) = context.dataStore.edit { it[Keys.downloadQuality] = height }
    suspend fun setDownloadWifiOnly(enabled: Boolean) = context.dataStore.edit { it[Keys.downloadWifiOnly] = enabled }
}
