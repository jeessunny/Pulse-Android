package com.pulse.telemetry.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSize: Long,
    val publishedAt: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Available(val release: AppReleaseInfo) : UpdateState
    data class Downloading(
        val release: AppReleaseInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState
    data class ReadyToInstall(
        val release: AppReleaseInfo,
        val apkFile: File,
        val needsPermission: Boolean = false
    ) : UpdateState
    data class Error(val message: String) : UpdateState
}

class AppUpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_REPO_API = "https://api.github.com/repos/jeessunny/Pulse-Android/releases/latest"
        private const val USER_AGENT = "Pulse-Android-App"
        private const val UPDATES_DIR = "updates"
        private const val APK_FILE_NAME = "pulse_update.apk"
        private const val TEMP_APK_FILE_NAME = "pulse_update_temp.apk"
    }

    suspend fun checkForUpdates(currentVersion: String): UpdateState = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_REPO_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateState.Error("GitHub responded with HTTP $responseCode")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").trim()
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            var apkUrl: String? = null
            var apkSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                return@withContext UpdateState.Error("No APK package attached to release $tagName")
            }

            val versionName = tagName.removePrefix("v").trim()
            val hasUpdate = isNewerVersion(tagName, currentVersion)

            if (hasUpdate) {
                val releaseInfo = AppReleaseInfo(
                    tagName = tagName,
                    versionName = versionName,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl,
                    assetSize = apkSize,
                    publishedAt = publishedAt
                )
                UpdateState.Available(releaseInfo)
            } else {
                UpdateState.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateState.Error("Could not reach GitHub: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun downloadUpdate(
        release: AppReleaseInfo,
        onProgress: (progress: Float, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
            // Clean up any stale APKs
            updatesDir.listFiles()?.forEach { it.delete() }

            val tempFile = File(updatesDir, TEMP_APK_FILE_NAME)
            val finalFile = File(updatesDir, APK_FILE_NAME)

            val connection = openConnectionWithRedirects(release.downloadUrl)
            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else release.assetSize

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastUpdateTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime >= 100) {
                            val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else -1f
                            onProgress(progress, totalRead, totalBytes)
                            lastUpdateTime = now
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.exists()) {
                if (finalFile.exists()) finalFile.delete()
                tempFile.renameTo(finalFile)
            }

            onProgress(1f, totalBytes, totalBytes)
            finalFile
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    fun startInstall(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false

        if (!canInstallPackages()) {
            return false
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openConnectionWithRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        val maxRedirects = 6

        while (redirects < maxRedirects) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308) {
                val redirectUrl = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                currentUrl = redirectUrl
                redirects++
            } else {
                return connection
            }
        }
        return URL(currentUrl).openConnection() as HttpURLConnection
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val cleanLatest = latest.removePrefix("v").trim()
        val cleanCurrent = current.removePrefix("v").trim()
        if (cleanLatest == cleanCurrent) return false

        val latestParts = cleanLatest.split(".").mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }
        val currentParts = cleanCurrent.split(".").mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
