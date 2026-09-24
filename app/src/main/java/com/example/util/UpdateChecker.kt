package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)

object UpdateChecker {
    private val client = OkHttpClient()

    suspend fun checkForUpdates(
        context: Context,
        currentVersionName: String,
        repoOwnerAndName: String = "mamunarman2004/CourseSchedule"
    ): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repoOwnerAndName/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "CourseSchedule-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        // If 404 or no releases published yet, fallback gracefully to up-to-date
                        return@withContext UpdateInfo(
                            latestVersion = currentVersionName,
                            releaseNotes = "You are up to date. (No remote releases found on GitHub repository yet).",
                            downloadUrl = "https://github.com/$repoOwnerAndName/releases",
                            isUpdateAvailable = false
                        )
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "1.0").removePrefix("v").removePrefix("V")
                    val bodyNotes = json.optString("body", "No release notes provided.")
                    val htmlUrl = json.optString("html_url", "https://github.com/$repoOwnerAndName/releases")

                    val currentClean = currentVersionName.removePrefix("v").removePrefix("V")
                    val isNewer = compareVersions(tagName, currentClean) > 0

                    UpdateInfo(
                        latestVersion = tagName,
                        releaseNotes = bodyNotes,
                        downloadUrl = htmlUrl,
                        isUpdateAvailable = isNewer
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback gracefully on network error/exception so it never fails awkwardly
                UpdateInfo(
                    latestVersion = currentVersionName,
                    releaseNotes = "Unable to connect to GitHub releases. Please check your internet connection.",
                    downloadUrl = "https://github.com/$repoOwnerAndName/releases",
                    isUpdateAvailable = false
                )
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    fun openDownloadPage(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
