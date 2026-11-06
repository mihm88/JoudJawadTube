package com.joudjawadtube.player.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.joudjawadtube.player.data.local.VideoDao
import com.joudjawadtube.player.data.local.VideoItem
import com.joudjawadtube.player.data.local.YouTubeIdExtractor
import com.joudjawadtube.player.data.settings.AppSettingsState
import com.joudjawadtube.player.data.settings.FrameCapturePosition
import com.joudjawadtube.player.data.settings.ThumbnailMode
import com.joudjawadtube.player.data.youtube.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ThumbnailManager {

    // Limit concurrent thumbnail network/USB operations so UI scrolling stays 60fps
    private val generationSemaphore = Semaphore(3)

    // In-memory negative lookup cache so we don't repeatedly search YouTube for offline/unknown files in one session
    private val failedYouTubeLookups = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun getThumbnailsDir(context: Context): File {
        val dir = File(context.filesDir, "thumbnails_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Fast synchronous check if a cached thumbnail file already exists on internal flash storage.
     */
    fun getCachedThumbnailFile(
        context: Context,
        video: VideoItem,
        settings: AppSettingsState
    ): File? {
        if (video.entity.customThumbnailPath.isNotBlank()) {
            val customFile = File(video.entity.customThumbnailPath)
            if (customFile.exists() && customFile.length() > 0L) return customFile
        }

        val dir = getThumbnailsDir(context)
        val ytFile = File(dir, "${video.id}_yt.jpg")
        val frameFile = File(dir, "${video.id}_frame.jpg")

        return when (settings.thumbnailMode) {
            ThumbnailMode.YOUTUBE_FIRST -> {
                when {
                    ytFile.exists() && ytFile.length() > 512L -> ytFile
                    frameFile.exists() && frameFile.length() > 512L && failedYouTubeLookups.contains(video.id) -> frameFile
                    else -> null
                }
            }
            ThumbnailMode.AUTO_FRAME_ONLY -> {
                if (frameFile.exists() && frameFile.length() > 512L) frameFile
                else if (ytFile.exists() && ytFile.length() > 512L && video.isYoutube) ytFile
                else null
            }
            ThumbnailMode.YOUTUBE_AND_SIDECAR_ONLY -> {
                if (ytFile.exists() && ytFile.length() > 512L) ytFile else null
            }
        }
    }

    /**
     * Resolves or generates a persistent local JPEG thumbnail following user priority:
     * 1. Custom user override (if set)
     * 2. Official YouTube Thumbnail (via extracted [videoId] in filename, saved youtubeVideoId, or YouTube title search)
     * 3. Sidecar cover image (.jpg/.webp/.png in video folder)
     * 4. Automatic video frame extraction via MediaMetadataRetriever at fast sync keyframe
     */
    suspend fun resolveOrGenerateThumbnail(
        context: Context,
        video: VideoItem,
        settings: AppSettingsState,
        videoDao: VideoDao? = null,
        forceRefresh: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            getCachedThumbnailFile(context, video, settings)?.let { return@withContext it }
        }

        val dir = getThumbnailsDir(context)
        val ytFile = File(dir, "${video.id}_yt.jpg")
        val frameFile = File(dir, "${video.id}_frame.jpg")

        generationSemaphore.withPermit {
            // Re-check inside semaphore
            if (!forceRefresh) {
                getCachedThumbnailFile(context, video, settings)?.let { return@withPermit it }
            }

            // STEP 1: Try YouTube Thumbnail First (unless user explicitly forced AUTO_FRAME_ONLY on a local file)
            val tryYouTubeFirst = settings.thumbnailMode != ThumbnailMode.AUTO_FRAME_ONLY || video.isYoutube
            if (tryYouTubeFirst && (forceRefresh || !failedYouTubeLookups.contains(video.id))) {
                val resolvedYtFile = fetchAndCacheYouTubeThumbnail(video, ytFile, videoDao)
                if (resolvedYtFile != null) {
                    return@withPermit resolvedYtFile
                } else {
                    failedYouTubeLookups.add(video.id)
                }
            }

            // If we already have a cached frameFile from an earlier fallback, return it immediately
            if (!forceRefresh && frameFile.exists() && frameFile.length() > 512L) {
                return@withPermit frameFile
            }

            // STEP 2: Check for sidecar cover image next to local video file
            findSidecarCoverImage(video)?.let { sidecarFile ->
                if (copyAndScaleImageFile(sidecarFile, frameFile)) {
                    return@withPermit frameFile
                }
            }

            // STEP 3: Automatic Frame Extraction from local/USB video (if not disabled)
            if (settings.thumbnailMode != ThumbnailMode.YOUTUBE_AND_SIDECAR_ONLY && !video.isYoutube) {
                if (extractVideoFrameToFile(context, video, settings.frameCapturePosition, frameFile)) {
                    return@withPermit frameFile
                }
            }

            // Final fallback: return whichever cached file exists
            when {
                ytFile.exists() && ytFile.length() > 512L -> ytFile
                frameFile.exists() && frameFile.length() > 512L -> frameFile
                else -> null
            }
        }
    }

    /**
     * Extracts a frame at a specific timestamp chosen manually by the user and sets it as custom thumbnail.
     */
    suspend fun generateCustomFrameThumbnailAt(
        context: Context,
        video: VideoItem,
        positionMs: Long,
        videoDao: VideoDao
    ): File? = withContext(Dispatchers.IO) {
        val dir = getThumbnailsDir(context)
        val customFile = File(dir, "${video.id}_custom_${System.currentTimeMillis()}.jpg")
        val ok = extractFrameAtExactMs(context, video, positionMs, customFile)
        if (ok && customFile.exists()) {
            videoDao.updateCustomThumbnail(video.id, customFile.absolutePath)
            customFile
        } else {
            null
        }
    }

    private suspend fun fetchAndCacheYouTubeThumbnail(
        video: VideoItem,
        targetFile: File,
        videoDao: VideoDao?
    ): File? {
        // 1. Check direct thumbnailUrl if already stored
        val explicitUrl = video.entity.thumbnailUrl.takeIf { it.startsWith("http") }
        if (explicitUrl != null && downloadImageToFile(explicitUrl, targetFile)) {
            return targetFile
        }

        // 2. Check known or filename-embedded 11-char YouTube ID (e.g. [IJT0Hk7d808].webm)
        var ytId = video.youtubeVideoId.takeIf { it.length == 11 }
            ?: YouTubeIdExtractor.extractFromFileNameOrUri(video.entity.fileName, video.entity.uri)

        // 3. If no [videoId] in filename, search YouTube by cleaned video title
        if (ytId.isNullOrBlank()) {
            ytId = YouTubeService.searchVideoIdByTitle(video.title)
        }

        if (ytId.isNullOrBlank()) return null

        val candidateUrls = listOf(
            "https://i.ytimg.com/vi/$ytId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$ytId/mqdefault.jpg"
        )

        for (url in candidateUrls) {
            if (downloadImageToFile(url, targetFile)) {
                runCatching {
                    videoDao?.updateResolvedYouTubeThumbnail(video.id, ytId, url)
                }
                return targetFile
            }
        }
        return null
    }

    private fun downloadImageToFile(urlString: String, targetFile: File): Boolean {
        return runCatching {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 7_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode !in 200..299) return false
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size < 1024) return false

            // Decode and re-save cropped/scaled 16:9 (remove 4:3 letterbox bars on hqdefault.jpg)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
            val cropped = cropTo16By9AndScale(decoded, 480, 270)
            val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            FileOutputStream(tmpFile).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 86, out)
            }
            if (cropped !== decoded) decoded.recycle()
            cropped.recycle()
            if (targetFile.exists()) targetFile.delete()
            tmpFile.renameTo(targetFile)
            targetFile.exists() && targetFile.length() > 512L
        }.getOrDefault(false)
    }

    private fun findSidecarCoverImage(video: VideoItem): File? {
        val path = video.entity.filePath.takeIf { it.isNotBlank() } ?: return null
        val videoFile = File(path)
        val dir = videoFile.parentFile ?: return null
        if (!dir.exists() || !dir.canRead()) return null

        val base = videoFile.nameWithoutExtension
        val candidates = listOf(
            "$base.jpg", "$base.jpeg", "$base.webp", "$base.png",
            "$base-thumb.jpg", "poster.jpg", "folder.jpg", "cover.jpg"
        )
        for (name in candidates) {
            val f = File(dir, name)
            if (f.exists() && f.isFile && f.length() > 1024L) {
                return f
            }
        }
        return null
    }

    private fun copyAndScaleImageFile(sourceFile: File, targetFile: File): Boolean {
        return runCatching {
            val bmp = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return false
            val scaled = cropTo16By9AndScale(bmp, 480, 270)
            FileOutputStream(targetFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bmp) bmp.recycle()
            scaled.recycle()
            targetFile.exists() && targetFile.length() > 512L
        }.getOrDefault(false)
    }

    private fun extractVideoFrameToFile(
        context: Context,
        video: VideoItem,
        capturePos: FrameCapturePosition,
        targetFile: File
    ): Boolean {
        val durationMs = video.durationMs
        val targetMs = when {
            capturePos.fraction != null && durationMs > 10_000L ->
                (durationMs * capturePos.fraction).toLong()
            durationMs > 5_000L -> 3_000L
            else -> 1_000L
        }
        return extractFrameAtExactMs(context, video, targetMs, targetFile)
    }

    private fun extractFrameAtExactMs(
        context: Context,
        video: VideoItem,
        positionMs: Long,
        targetFile: File
    ): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            val filePath = video.entity.filePath
            if (filePath.isNotBlank() && File(filePath).exists()) {
                retriever.setDataSource(filePath)
            } else {
                retriever.setDataSource(context, Uri.parse(video.entity.uri))
            }

            val timeUs = positionMs.coerceAtLeast(500L) * 1000L
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return false

            val scaled = cropTo16By9AndScale(frame, 480, 270)
            val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            FileOutputStream(tmpFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 84, out)
            }
            if (scaled !== frame) frame.recycle()
            scaled.recycle()
            if (targetFile.exists()) targetFile.delete()
            tmpFile.renameTo(targetFile)
            targetFile.exists() && targetFile.length() > 512L
        } catch (_: Exception) {
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun cropTo16By9AndScale(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src

        val targetAspect = 16f / 9f
        val currentAspect = w.toFloat() / h.toFloat()

        val cropped = if (kotlin.math.abs(currentAspect - targetAspect) > 0.08f) {
            if (currentAspect < targetAspect) {
                // Source is taller (e.g. 4:3 YouTube hqdefault with top/bottom black bars) -> crop top & bottom
                val newH = (w / targetAspect).toInt().coerceIn(1, h)
                val yOffset = ((h - newH) / 2).coerceAtLeast(0)
                Bitmap.createBitmap(src, 0, yOffset, w, newH)
            } else {
                val newW = (h * targetAspect).toInt().coerceIn(1, w)
                val xOffset = ((w - newW) / 2).coerceAtLeast(0)
                Bitmap.createBitmap(src, xOffset, 0, newW, h)
            }
        } else {
            src
        }

        val scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
        if (cropped !== src && cropped !== scaled) {
            cropped.recycle()
        }
        return scaled
    }

    fun getCacheSizeBytes(context: Context): Long {
        val dir = getThumbnailsDir(context)
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearThumbnailCache(context: Context) {
        failedYouTubeLookups.clear()
        val dir = getThumbnailsDir(context)
        dir.listFiles()?.forEach { runCatching { it.delete() } }
    }
}
