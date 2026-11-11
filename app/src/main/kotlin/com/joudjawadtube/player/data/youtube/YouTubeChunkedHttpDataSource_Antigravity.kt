package com.joudjawadtube.player.data.youtube

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import kotlin.math.min

/**
 * Custom Media3 HTTP DataSource that bypasses YouTube CDN (googlevideo.com) bandwidth throttling
 * (~329 KB/s on plain GET without range) by splitting reads into unthrottled `&range=start-end`
 * chunk requests (~5,000+ KB/s), while presenting a seamless continuous stream to ExoPlayer.
 */
@OptIn(UnstableApi::class)
class YouTubeChunkedHttpDataSource_Antigravity(
    private val userAgent: String,
    private val connectTimeoutMs: Int = 6_000,
    private val readTimeoutMs: Int = 8_000
) : BaseDataSource(/* isNetwork = */ true) {

    class Factory(
        private val userAgent: String,
        private val connectTimeoutMs: Int = 6_000,
        private val readTimeoutMs: Int = 8_000
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return YouTubeChunkedHttpDataSource_Antigravity(
                userAgent = userAgent,
                connectTimeoutMs = connectTimeoutMs,
                readTimeoutMs = readTimeoutMs
            )
        }
    }

    companion object {
        // Fast initial chunks for sub-300ms Time-To-First-Frame
        private const val INITIAL_VIDEO_CHUNK_BYTES = 512 * 1024L
        private const val INITIAL_AUDIO_CHUNK_BYTES = 256 * 1024L
        // Larger subsequent chunks (2.5 MB) to minimize HTTP round-trips during steady playback
        private const val SUBSEQUENT_VIDEO_CHUNK_BYTES = 2_560 * 1024L
        private const val SUBSEQUENT_AUDIO_CHUNK_BYTES = 1_024 * 1024L
    }

    private var delegate: DefaultHttpDataSource? = null
    private var baseDataSpec: DataSpec? = null
    private var currentUri: Uri? = null
    private var isChunkedRangeMode: Boolean = false
    private var isAudioStream: Boolean = false
    private var currentReadPosition: Long = 0L
    private var totalBytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var currentChunkBytesRemaining: Long = 0L
    private var isFirstChunk: Boolean = true
    private var opened: Boolean = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        val httpDelegate = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(connectTimeoutMs)
            .setReadTimeoutMs(readTimeoutMs)
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()

        delegate = httpDelegate
        baseDataSpec = dataSpec
        currentUri = dataSpec.uri

        val uri = dataSpec.uri
        val rawUriStr = uri.toString()
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty()
        val clen = extractClenFromUri(uri, rawUriStr)
        val isHls = path.endsWith(".m3u8", ignoreCase = true) ||
            rawUriStr.contains("/playlist/index.m3u8") ||
            rawUriStr.contains("manifest.googlevideo.com")

        if (host.contains("googlevideo.com") && clen > 0L && !isHls) {
            isChunkedRangeMode = true
            isAudioStream = rawUriStr.contains("mime=audio", ignoreCase = true)
            currentReadPosition = dataSpec.position.coerceAtLeast(0L)
            totalBytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                (clen - currentReadPosition).coerceAtLeast(0L)
            }
            isFirstChunk = true
            currentChunkBytesRemaining = 0L

            if (totalBytesRemaining > 0L) {
                openNextChunk()
            }
            opened = true
            transferStarted(dataSpec)
            return totalBytesRemaining
        } else {
            isChunkedRangeMode = false
            val len = httpDelegate.open(dataSpec)
            currentUri = httpDelegate.uri ?: dataSpec.uri
            opened = true
            transferStarted(dataSpec)
            return len
        }
    }

    private fun openNextChunk() {
        val httpDelegate = delegate ?: return
        val origSpec = baseDataSpec ?: return
        try {
            httpDelegate.close()
        } catch (_: Throwable) {}

        val chunkCap = when {
            isFirstChunk && isAudioStream -> INITIAL_AUDIO_CHUNK_BYTES
            isFirstChunk -> INITIAL_VIDEO_CHUNK_BYTES
            isAudioStream -> SUBSEQUENT_AUDIO_CHUNK_BYTES
            else -> SUBSEQUENT_VIDEO_CHUNK_BYTES
        }
        isFirstChunk = false

        val chunkLength = min(chunkCap, totalBytesRemaining)
        if (chunkLength <= 0L) {
            currentChunkBytesRemaining = 0L
            return
        }

        val chunkStart = currentReadPosition
        val chunkEnd = chunkStart + chunkLength - 1L
        val chunkUri = appendOrReplaceRangeParam(origSpec.uri, chunkStart, chunkEnd)

        val chunkSpec = origSpec.buildUpon()
            .setUri(chunkUri)
            .setPosition(0L)
            .setLength(C.LENGTH_UNSET.toLong())
            .build()

        val returnedLen = httpDelegate.open(chunkSpec)
        currentUri = httpDelegate.uri ?: chunkUri
        currentChunkBytesRemaining = if (returnedLen != C.LENGTH_UNSET.toLong() && returnedLen > 0L) {
            min(returnedLen, chunkLength)
        } else {
            chunkLength
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val httpDelegate = delegate ?: return C.RESULT_END_OF_INPUT

        if (!isChunkedRangeMode) {
            val read = httpDelegate.read(buffer, offset, length)
            if (read > 0) {
                bytesTransferred(read)
            }
            return read
        }

        if (totalBytesRemaining <= 0L) {
            return C.RESULT_END_OF_INPUT
        }

        if (currentChunkBytesRemaining <= 0L) {
            openNextChunk()
            if (currentChunkBytesRemaining <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
        }

        val toRead = min(length.toLong(), min(currentChunkBytesRemaining, totalBytesRemaining)).toInt()
        val bytesRead = httpDelegate.read(buffer, offset, toRead)
        if (bytesRead == C.RESULT_END_OF_INPUT) {
            currentChunkBytesRemaining = 0L
            if (totalBytesRemaining <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
            openNextChunk()
            if (currentChunkBytesRemaining <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
            val retryToRead = min(length.toLong(), min(currentChunkBytesRemaining, totalBytesRemaining)).toInt()
            val retryRead = httpDelegate.read(buffer, offset, retryToRead)
            if (retryRead == C.RESULT_END_OF_INPUT) {
                totalBytesRemaining = 0L
                return C.RESULT_END_OF_INPUT
            }
            currentReadPosition += retryRead
            currentChunkBytesRemaining -= retryRead
            totalBytesRemaining -= retryRead
            bytesTransferred(retryRead)
            return retryRead
        }

        currentReadPosition += bytesRead
        currentChunkBytesRemaining -= bytesRead
        totalBytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        try {
            delegate?.close()
        } catch (_: Throwable) {
        } finally {
            delegate = null
            currentChunkBytesRemaining = 0L
            totalBytesRemaining = 0L
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private fun extractClenFromUri(uri: Uri, rawUriStr: String): Long {
        uri.getQueryParameter("clen")?.toLongOrNull()?.let { if (it > 0L) return it }
        val match = Regex("""[?&]clen=(\d+)""").find(rawUriStr)
        return match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: -1L
    }

    private fun appendOrReplaceRangeParam(uri: Uri, start: Long, end: Long): Uri {
        val raw = uri.toString().replace(Regex("""([?&])range=\d+-\d+&?""")) { match ->
            if (match.value.endsWith("&")) match.groupValues[1] else ""
        }.trimEnd('&', '?')
        val sep = if (raw.contains('?')) '&' else '?'
        return Uri.parse("${raw}${sep}range=${start}-${end}")
    }
}
