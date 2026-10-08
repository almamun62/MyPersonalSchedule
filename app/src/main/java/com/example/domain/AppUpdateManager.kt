package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AppUpdateManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Default GitHub repository owner and name (can be configured or customized in settings)
    const val DEFAULT_GITHUB_REPO = "mamunarman2004/CourseSchedule"

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun cleanVersionString(v: String): String {
        return v.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Compares version strings like "1.0.1" vs "1.0.0" or "2.0" vs "1.9".
     * Returns true if latest > current.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currClean = cleanVersionString(current)
        val lateClean = cleanVersionString(latest)

        val currParts = currClean.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = lateClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Checks the GitHub Releases API for the latest release:
     * GET https://api.github.com/repos/{owner}/{repo}/releases/latest
     */
    suspend fun checkForUpdates(
        context: Context,
        repoSlug: String = DEFAULT_GITHUB_REPO
    ): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val cleanSlug = repoSlug.trim().removePrefix("https://github.com/").removeSuffix("/")

            val url = "https://api.github.com/repos/$cleanSlug/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "CourseSchedule-AndroidApp")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch release (HTTP ${response.code}). Ensure repo '$cleanSlug' is public with published releases.")
                )
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response received from GitHub Releases API.")
            )

            val release = json.decodeFromString<GitHubRelease>(responseBody)
            val latestVersionTag = release.tagName
            val hasUpdate = isNewerVersion(currentVersion, latestVersionTag)

            // Find APK asset in release if uploaded
            val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }

            Result.success(
                UpdateCheckResult(
                    hasUpdate = hasUpdate,
                    currentVersion = currentVersion,
                    latestVersion = latestVersionTag,
                    releaseNotes = release.body.ifBlank { release.name.ifBlank { "No release notes provided." } },
                    releaseUrl = release.htmlUrl,
                    apkDownloadUrl = apkAsset?.browserDownloadUrl,
                    apkName = apkAsset?.name ?: "CourseSchedule-$latestVersionTag.apk",
                    apkSize = apkAsset?.size ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK file to the app's cache directory and streams progress (0..100).
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "CourseSchedule-AndroidApp")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download body"))
            val contentLength = body.contentLength()

            val downloadDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outputFile = File(downloadDir, fileName)

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Triggers the Android package installer Intent using FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Open file directly
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
