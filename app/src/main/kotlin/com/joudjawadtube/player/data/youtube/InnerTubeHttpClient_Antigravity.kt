package com.joudjawadtube.player.data.youtube

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/**
 * Shared OkHttp client for InnerTube requests with coroutine cancellation support
 * so losing client race calls are aborted on the socket immediately.
 */
object InnerTubeHttpClient_Antigravity {

    private const val DEFAULT_WEB_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val probeClient: OkHttpClient = client.newBuilder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    suspend fun executeCancellable(
        request: Request,
        customClient: OkHttpClient = client
    ): Response = suspendCancellableCoroutine { cont ->
        val call = customClient.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) {
                    @Suppress("DEPRECATION")
                    cont.resume(response) {
                        response.close()
                    }
                } else {
                    response.close()
                }
            }
        })
    }

    suspend fun postJson(
        url: String,
        jsonBody: String,
        userAgent: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String? {
        val builder = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
        for ((key, value) in extraHeaders) {
            builder.header(key, value)
        }
        return try {
            executeCancellable(builder.build()).use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getString(
        url: String,
        userAgent: String = DEFAULT_WEB_UA
    ): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .build()
        return try {
            executeCancellable(request).use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            null
        }
    }

    fun getStringBlocking(
        url: String,
        userAgent: String = DEFAULT_WEB_UA
    ): String? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull()
    }

    suspend fun probeRangeOk(
        url: String,
        userAgent: String
    ): Boolean {
        val sep = if (url.contains('?')) '&' else '?'
        val request = Request.Builder()
            .url("${url}${sep}range=0-1023")
            .get()
            .header("User-Agent", userAgent)
            .build()
        return try {
            executeCancellable(request, probeClient).use { response ->
                response.isSuccessful
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            false
        }
    }
}
