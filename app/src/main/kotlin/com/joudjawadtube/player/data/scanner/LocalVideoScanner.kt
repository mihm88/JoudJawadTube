package com.joudjawadtube.player.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MimeTypes
import com.joudjawadtube.player.data.local.SavedSafFolderEntity
import com.joudjawadtube.player.data.local.SidecarSubtitle
import com.joudjawadtube.player.data.local.SubtitleCodec
import com.joudjawadtube.player.data.local.VideoEntity
import com.joudjawadtube.player.data.local.YouTubeIdExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object LocalVideoScanner {

    val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "m2ts", "flv", "3gp", "wmv", "mpg", "mpeg"
    )

    val SUBTITLE_EXTENSIONS = mapOf(
        "srt" to MimeTypes.APPLICATION_SUBRIP,
        "vtt" to MimeTypes.TEXT_VTT,
        "ass" to MimeTypes.TEXT_SSA,
        "ssa" to MimeTypes.TEXT_SSA
    )

    private val IGNORED_DIR_NAMES = setOf(
        "Android", ".thumbnails", ".trash", ".Trashes", "System Volume Information", "\$RECYCLE.BIN",
        "lost+found", "LOST.DIR", "EFI", "grub", "ventoy"
    )

    suspend fun scanAllSources(
        context: Context,
        safFolders: List<SavedSafFolderEntity>,
        hideBootPartitions: Boolean = true
    ): List<VideoEntity> = withContext(Dispatchers.IO) {
        val volumes = StorageVolumeHelper.getMountedVolumes(context, hideBootPartitions)
        val discoveredByPathOrUri = linkedMapOf<String, VideoEntity>()
        val directorySubtitleCache = hashMapOf<String, List<Pair<String, String>>>()

        // Tier 1: Query MediaStore.Video.Media (fast metadata for indexed internal & portable storage)
        runCatching {
            val mediaStoreVideos = queryMediaStoreVideos(context, volumes, directorySubtitleCache)
            for (video in mediaStoreVideos) {
                val key = video.filePath.ifBlank { video.uri }
                discoveredByPathOrUri[key] = video
            }
        }

        // Tier 2: Direct filesystem walk across all readable internal & USB OTG volumes
        for (volume in volumes) {
            if (volume.isBootOrSystemPartition && hideBootPartitions) continue
            val rootDir = File(volume.path)
            if (rootDir.exists() && rootDir.canRead()) {
                runCatching {
                    walkDirectoryForVideos(rootDir, volume, discoveredByPathOrUri, maxDepth = 8)
                }
            }
        }

        // Tier 3: Walk user-added SAF DocumentTree URIs or direct local folder paths
        for (safFolder in safFolders) {
            runCatching {
                if (safFolder.uriString.startsWith("/")) {
                    // Direct folder path added via in-app TV Folder Picker
                    val directDir = File(safFolder.uriString)
                    if (directDir.exists() && directDir.canRead()) {
                        val volInfo = StorageVolumeHelper.classifyFilePath(directDir.absolutePath, volumes)
                        walkDirectoryForVideos(directDir, volInfo, discoveredByPathOrUri, maxDepth = 6)
                    }
                } else {
                    val treeUri = Uri.parse(safFolder.uriString)
                    val docRoot = DocumentFile.fromTreeUri(context, treeUri)
                    if (docRoot != null && docRoot.exists() && docRoot.canRead()) {
                        walkSafDocumentTree(context, docRoot, safFolder.displayName, discoveredByPathOrUri, maxDepth = 6)
                    }
                }
            }
        }

        discoveredByPathOrUri.values.sortedWith(
            compareByDescending<VideoEntity> { it.isUsb }
                .thenBy { it.folderName.lowercase(Locale.ROOT) }
                .thenBy { it.title.lowercase(Locale.ROOT) }
        )
    }

    private fun queryMediaStoreVideos(
        context: Context,
        volumes: List<MountedVolumeInfo>,
        directorySubtitleCache: MutableMap<String, List<Pair<String, String>>>
    ): List<VideoEntity> {
        val results = mutableListOf<VideoEntity>()
        val contentUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).map { volName ->
                MediaStore.Video.Media.getContentUri(volName)
            }.ifEmpty { listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) }
        } else {
            listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        for (queryUri in contentUris) {
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(queryUri, mediaId).toString()
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val fileObj = if (filePath.isNotBlank()) File(filePath) else null
                    val fileName = (if (nameCol >= 0) cursor.getString(nameCol) else null)
                        ?: fileObj?.name
                        ?: "Video_$mediaId.mp4"
                    val rawTitle = (if (titleCol >= 0) cursor.getString(titleCol) else null)
                        ?: fileName.substringBeforeLast('.')
                    val durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else (fileObj?.length() ?: 0L)
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val mimeType = (if (mimeCol >= 0) cursor.getString(mimeCol) else null) ?: "video/mp4"
                    val dateMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else (fileObj?.lastModified() ?: 0L)

                    val parentFile = fileObj?.parentFile
                    val folderPath = parentFile?.absolutePath ?: "MediaStore"
                    val folderName = parentFile?.name?.takeIf { it.isNotBlank() } ?: "Movies"
                    val volInfo = if (filePath.isNotBlank()) {
                        StorageVolumeHelper.classifyFilePath(filePath, volumes)
                    } else {
                        MountedVolumeInfo("primary", "Internal Storage", "/storage/emulated/0", false)
                    }

                    // Cache subtitle listing once per folder so USB folders aren't re-listed 59 times
                    val sidecars = if (parentFile != null) {
                        val siblings = directorySubtitleCache.getOrPut(folderPath) {
                            if (parentFile.exists() && parentFile.canRead()) {
                                listSubtitlesInDirectoryOnce(parentFile)
                            } else {
                                emptyList()
                            }
                        }
                        matchSidecarSubtitles(fileName, siblings)
                    } else {
                        emptyList()
                    }

                    val extractedYtId = YouTubeIdExtractor.extractFromFileNameOrUri(fileName).orEmpty()
                    val ytThumbUrl = if (extractedYtId.isNotBlank()) {
                        YouTubeIdExtractor.buildYouTubeThumbnailUrl(extractedYtId)
                    } else {
                        ""
                    }

                    val uniqueKey = filePath.ifBlank { contentUri }
                    results.add(
                        VideoEntity(
                            id = sha256Short(uniqueKey),
                            videoKey = buildStableVideoKey(fileName, sizeBytes),
                            uri = contentUri,
                            filePath = filePath,
                            title = prettifyVideoTitle(rawTitle),
                            fileName = fileName,
                            folderName = folderName,
                            folderPath = folderPath,
                            volumeLabel = volInfo.label,
                            isUsb = volInfo.isUsbOrRemovable,
                            durationMs = durationMs,
                            sizeBytes = sizeBytes,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            dateModified = dateMod,
                            sidecarSubtitles = SubtitleCodec.encode(sidecars),
                            youtubeVideoId = extractedYtId,
                            thumbnailUrl = ytThumbUrl
                        )
                    )
                }
            }
        }
        return results
    }

    private fun walkDirectoryForVideos(
        dir: File,
        volume: MountedVolumeInfo,
        sink: MutableMap<String, VideoEntity>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth > maxDepth) return
        if (dir.name.startsWith(".") || dir.name in IGNORED_DIR_NAMES) return

        val children = dir.listFiles() ?: return
        val filesInDir = children.filter { it.isFile }
        val subDirs = children.filter { it.isDirectory }

        val subtitleSiblings = filesInDir.mapNotNull { f ->
            val ext = f.extension.lowercase(Locale.ROOT)
            if (ext in SUBTITLE_EXTENSIONS) {
                f.name to Uri.fromFile(f).toString()
            } else {
                null
            }
        }

        for (file in filesInDir) {
            val ext = file.extension.lowercase(Locale.ROOT)
            if (ext !in VIDEO_EXTENSIONS) continue
            if (file.length() < 64 * 1024L) continue

            val absPath = file.absolutePath
            val existing = sink[absPath]
            val sidecars = matchSidecarSubtitles(file.name, subtitleSiblings)

            if (existing != null) {
                if (existing.sidecarSubtitles.isBlank() && sidecars.isNotEmpty()) {
                    sink[absPath] = existing.copy(
                        sidecarSubtitles = SubtitleCodec.encode(sidecars),
                        isUsb = volume.isUsbOrRemovable,
                        volumeLabel = volume.label
                    )
                }
                continue
            }

            val meta = probeVideoMetadataFast(absPath)
            val folderName = dir.name.takeIf { it.isNotBlank() && it != volume.id } ?: volume.label
            val extractedYtId = YouTubeIdExtractor.extractFromFileNameOrUri(file.name).orEmpty()
            val ytThumbUrl = if (extractedYtId.isNotBlank()) {
                YouTubeIdExtractor.buildYouTubeThumbnailUrl(extractedYtId)
            } else {
                ""
            }

            sink[absPath] = VideoEntity(
                id = sha256Short(absPath),
                videoKey = buildStableVideoKey(file.name, file.length()),
                uri = Uri.fromFile(file).toString(),
                filePath = absPath,
                title = prettifyVideoTitle(file.nameWithoutExtension),
                fileName = file.name,
                folderName = folderName,
                folderPath = dir.absolutePath,
                volumeLabel = volume.label,
                isUsb = volume.isUsbOrRemovable,
                durationMs = meta.durationMs,
                sizeBytes = file.length(),
                width = meta.width,
                height = meta.height,
                mimeType = guessVideoMimeType(ext),
                dateModified = file.lastModified(),
                sidecarSubtitles = SubtitleCodec.encode(sidecars),
                youtubeVideoId = extractedYtId,
                thumbnailUrl = ytThumbUrl
            )
        }

        for (subDir in subDirs) {
            walkDirectoryForVideos(subDir, volume, sink, maxDepth, currentDepth + 1)
        }
    }

    private fun walkSafDocumentTree(
        context: Context,
        dir: DocumentFile,
        rootDisplayName: String,
        sink: MutableMap<String, VideoEntity>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth > maxDepth) return
        val dirName = dir.name ?: rootDisplayName
        if (dirName.startsWith(".") || dirName in IGNORED_DIR_NAMES) return

        val children = dir.listFiles()
        val files = children.filter { it.isFile }
        val subDirs = children.filter { it.isDirectory }

        val subtitleSiblings = files.mapNotNull { f ->
            val name = f.name ?: return@mapNotNull null
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (ext in SUBTITLE_EXTENSIONS) {
                name to f.uri.toString()
            } else {
                null
            }
        }

        for (doc in files) {
            val name = doc.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (ext !in VIDEO_EXTENSIONS) continue

            val uriStr = doc.uri.toString()
            if (sink.containsKey(uriStr)) continue

            val sidecars = matchSidecarSubtitles(name, subtitleSiblings)
            val size = doc.length()
            val extractedYtId = YouTubeIdExtractor.extractFromFileNameOrUri(name).orEmpty()
            val ytThumbUrl = if (extractedYtId.isNotBlank()) {
                YouTubeIdExtractor.buildYouTubeThumbnailUrl(extractedYtId)
            } else {
                ""
            }

            sink[uriStr] = VideoEntity(
                id = sha256Short(uriStr),
                videoKey = buildStableVideoKey(name, size),
                uri = uriStr,
                filePath = "",
                title = prettifyVideoTitle(name.substringBeforeLast('.')),
                fileName = name,
                folderName = dirName,
                folderPath = dir.uri.toString(),
                volumeLabel = "SAF: $rootDisplayName",
                isUsb = true,
                durationMs = 0L,
                sizeBytes = size,
                width = 0,
                height = 0,
                mimeType = doc.type ?: guessVideoMimeType(ext),
                dateModified = doc.lastModified(),
                sidecarSubtitles = SubtitleCodec.encode(sidecars),
                youtubeVideoId = extractedYtId,
                thumbnailUrl = ytThumbUrl
            )
        }

        for (subDir in subDirs) {
            walkSafDocumentTree(context, subDir, rootDisplayName, sink, maxDepth, currentDepth + 1)
        }
    }

    private fun listSubtitlesInDirectoryOnce(dir: File): List<Pair<String, String>> {
        return dir.listFiles()?.mapNotNull { f ->
            if (!f.isFile) return@mapNotNull null
            val ext = f.extension.lowercase(Locale.ROOT)
            if (ext in SUBTITLE_EXTENSIONS) {
                f.name to Uri.fromFile(f).toString()
            } else {
                null
            }
        } ?: emptyList()
    }

    fun findSidecarSubtitlesInDirectory(videoFileName: String, dir: File): List<SidecarSubtitle> {
        return matchSidecarSubtitles(videoFileName, listSubtitlesInDirectoryOnce(dir))
    }

    fun matchSidecarSubtitles(
        videoFileName: String,
        siblingSubtitles: List<Pair<String, String>>
    ): List<SidecarSubtitle> {
        val videoBase = videoFileName.substringBeforeLast('.')
        if (videoBase.isBlank()) return emptyList()

        val matched = mutableListOf<SidecarSubtitle>()
        for ((subFileName, subUri) in siblingSubtitles) {
            val ext = subFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            val mimeType = SUBTITLE_EXTENSIONS[ext] ?: continue
            val subWithoutExt = subFileName.substringBeforeLast('.')

            val isExactMatch = subWithoutExt.equals(videoBase, ignoreCase = true)
            val isLangVariant = subWithoutExt.startsWith("$videoBase.", ignoreCase = true) ||
                subWithoutExt.startsWith("${videoBase}_", ignoreCase = true)

            if (isExactMatch || isLangVariant) {
                val langSuffix = if (isLangVariant && subWithoutExt.length > videoBase.length + 1) {
                    subWithoutExt.substring(videoBase.length + 1)
                } else {
                    "und"
                }
                val label = if (langSuffix == "und" || langSuffix.isBlank()) {
                    "External (${ext.uppercase(Locale.ROOT)})"
                } else {
                    "External - ${langSuffix.uppercase(Locale.ROOT)} (${ext.uppercase(Locale.ROOT)})"
                }
                matched.add(
                    SidecarSubtitle(
                        uriOrPath = subUri,
                        mimeType = mimeType,
                        language = langSuffix.lowercase(Locale.ROOT),
                        label = label
                    )
                )
            }
        }
        return matched
    }

    private data class ProbedMeta(val durationMs: Long, val width: Int, val height: Int)

    private fun probeVideoMetadataFast(filePath: String): ProbedMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            ProbedMeta(dur, w, h)
        } catch (_: Exception) {
            ProbedMeta(0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun prettifyVideoTitle(rawName: String): String {
        return rawName
            .replace(Regex("""\s*\[[a-zA-Z0-9_-]{11}\]\s*$"""), "") // Clean trailing [YouTubeID]
            .replace('.', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { rawName }
    }

    fun buildStableVideoKey(fileName: String, sizeBytes: Long): String {
        return "${fileName.lowercase(Locale.ROOT)}_$sizeBytes"
    }

    private fun guessVideoMimeType(ext: String): String = when (ext) {
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "ts", "m2ts" -> "video/mp2t"
        else -> "video/mp4"
    }

    fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
