package com.fuwanime.data

import com.fuwanime.BuildConfig
import com.fuwanime.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-app update checker against the GitHub Releases page. The changelog (release body)
 * is shown inside the app so users never have to open a GitHub compare view.
 */
class UpdateRepository(private val settingsStore: SettingsStore) {

    data class UpdateInfo(
        val tag: String,
        val title: String,
        val notes: String,
        val apkUrl: String?,
        val pageUrl: String,
    )

    /** The release tag this build was produced from, e.g. "build-8" or "v2.1.0". */
    private fun currentTag(): String {
        val versionName = BuildConfig.VERSION_NAME
        return if (versionName.contains("build.")) {
            "build-" + versionName.substringAfterLast('.')
        } else {
            "v$versionName"
        }
    }

    /** Returns update info when a newer release exists, null when up to date (or on failure). */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val currentTag = currentTag()
        if (!settingsStore.shouldCheckUpdate(currentTag)) return@withContext null
        val release = GitHubApi.latest() ?: return@withContext null
        settingsStore.markUpdateChecked()
        if (release.tagName == currentTag) return@withContext null

        UpdateInfo(
            tag = release.tagName,
            title = release.name?.takeIf { it.isNotBlank() } ?: "FuwaStream ${release.tagName.removePrefix("v")}",
            notes = release.body.orEmpty().trim(),
            apkUrl = release.assets.lastOrNull { it.name.endsWith(".apk") }?.url
                ?: release.assets.firstOrNull()?.url,
            pageUrl = release.htmlUrl,
        )
    }
}
