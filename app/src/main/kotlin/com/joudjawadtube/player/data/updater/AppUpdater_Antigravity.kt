package com.joudjawadtube.player.data.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AppUpdater_Antigravity"
private const val GITHUB_REPO_OWNER = "mihm88"
private const val GITHUB_REPO_NAME = "JoudJawadTube"
private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"

data class UpdateInfo_Antigravity(
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileName: String,
    val fileSize: Long,
    val isNewer: Boolean
)

sealed interface UpdateState_Antigravity {
    object Idle : UpdateState_Antigravity
    object Checking : UpdateState_Antigravity
    data class UpdateAvailable(val info: UpdateInfo_Antigravity) : UpdateState_Antigravity
    data class UpToDate(val currentVersion: String) : UpdateState_Antigravity
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateState_Antigravity
    data class ReadyToInstall(val apkFile: File, val info: UpdateInfo_Antigravity) : UpdateState_Antigravity
    data class Error(val message: String) : UpdateState_Antigravity
}

class AppUpdater_Antigravity(private val context: Context) {

    /**
     * Checks the GitHub Releases API for the latest published release.
     * Compares the release tagName with currentVersionName.
     */
    suspend fun checkForUpdate(currentVersionName: String): Result<UpdateInfo_Antigravity?> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Checking for update at $LATEST_RELEASE_API_URL, current version: $currentVersionName")
            val url = URL(LATEST_RELEASE_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "JoudJawadTube-AppUpdater")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "No releases published yet on repository $GITHUB_REPO_OWNER/$GITHUB_REPO_NAME")
                return@runCatching null
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("GitHub API responded with code $responseCode: $errorBody")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseJson = JSONObject(responseBody)

            val tagName = releaseJson.optString("tag_name", "").trim()
            val releaseTitle = releaseJson.optString("name", tagName).ifBlank { tagName }
            val releaseNotes = releaseJson.optString("body", "No release notes provided.").trim()

            val assetsArray = releaseJson.optJSONArray("assets") ?: org.json.JSONArray()
            var apkDownloadUrl: String? = null
            var apkFileName = "JoudJawadTube-update.apk"
            var apkFileSize = 0L

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url")
                    apkFileName = assetName
                    apkFileSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkDownloadUrl.isNullOrBlank()) {
                Log.w(TAG, "Release $tagName found, but no APK asset attached")
                return@runCatching null
            }

            val isNewer = compareVersions(latest = tagName, current = currentVersionName) > 0

            UpdateInfo_Antigravity(
                tagName = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = apkDownloadUrl,
                fileName = apkFileName,
                fileSize = apkFileSize,
                isNewer = isNewer
            )
        }
    }

    /**
     * Downloads the APK file to app cache directory following any HTTP redirects (e.g. GitHub to AWS S3).
     */
    suspend fun downloadApk(
        info: UpdateInfo_Antigravity,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updatesDir, info.fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentUrl = info.downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            val maxRedirects = 6

            while (true) {
                if (!currentUrl.startsWith("https://", ignoreCase = true)) {
                    throw SecurityException("Insecure download URL rejected: $currentUrl (HTTPS required)")
                }
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 25000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "JoudJawadTube-AppUpdater")
                    setRequestProperty("Accept", "*/*")
                }

                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location != null && redirectCount < maxRedirects) {
                        currentUrl = location
                        redirectCount++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else info.fileSize
            var downloadedBytes = 0L

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(percent, downloadedBytes, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            Log.d(TAG, "Downloaded update successfully to ${destinationFile.absolutePath} (${destinationFile.length()} bytes)")
            destinationFile
        }
    }

    /**
     * Launches the system package installer for the downloaded APK using AndroidX FileProvider.
     */
    fun installApk(apkFile: File): Result<Unit> = runCatching {
        if (!apkFile.exists()) {
            throw IllegalArgumentException("APK file does not exist: ${apkFile.absolutePath}")
        }

        // Verify package archive integrity
        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
        if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
            throw SecurityException("Downloaded update package is invalid or does not match ${context.packageName}")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    companion object {
        /**
         * Compares two semantic version strings like "v1.2.3" and "1.2.0".
         * Returns > 0 if latest > current, < 0 if latest < current, 0 if equal.
         */
        fun compareVersions(latest: String, current: String): Int {
            fun parse(v: String): List<Int> {
                return v.trim()
                    .removePrefix("v")
                    .removePrefix("V")
                    .split('-', '+')[0] // remove prerelease / build tags
                    .split('.')
                    .map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            }

            val vLatest = parse(latest)
            val vCurrent = parse(current)
            val maxLen = maxOf(vLatest.size, vCurrent.size)

            for (i in 0 until maxLen) {
                val partLatest = vLatest.getOrElse(i) { 0 }
                val partCurrent = vCurrent.getOrElse(i) { 0 }
                if (partLatest != partCurrent) {
                    return partLatest.compareTo(partCurrent)
                }
            }
            return 0
        }
    }
}
