package com.joudjawadtube.player.data.youtube

import android.content.Context
import org.json.JSONObject
import java.io.File

data class InnerTubeClientProfile_Antigravity(
    val clientName: String,
    val clientVersion: String,
    val userAgent: String,
    val clientNameHeaderId: String? = null,
    val extraClientFields: Map<String, Any> = emptyMap()
)

/**
 * Loads InnerTube client metadata from `innertube_clients_Antigravity.json` (or cached
 * filesDir copy) with hardcoded fallback defaults for JVM tests and cold starts.
 */
object InnerTubeClientConfig_Antigravity {

    private const val ASSET_FILE_NAME = "innertube_clients_Antigravity.json"
    private const val CACHE_FILE_NAME = "innertube_clients_cache_Antigravity.json"
    private const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L

    @Volatile
    private var cachedClients: Map<String, InnerTubeClientProfile_Antigravity>? = null

    fun defaultClients(): Map<String, InnerTubeClientProfile_Antigravity> = mapOf(
        "IOS_KIDS" to InnerTubeClientProfile_Antigravity(
            clientName = "IOS_KIDS",
            clientVersion = "9.40.0",
            userAgent = "com.google.ios.youtubekids/9.40.0 (iPad14,1; U; CPU iOS 18_1_0 like Mac OS X;)",
            clientNameHeaderId = "6",
            extraClientFields = mapOf(
                "deviceMake" to "Apple",
                "deviceModel" to "iPad14,1",
                "osName" to "iOS",
                "osVersion" to "18.1.0.22B83"
            )
        ),
        "ANDROID_KIDS" to InnerTubeClientProfile_Antigravity(
            clientName = "ANDROID_KIDS",
            clientVersion = "9.40.00",
            userAgent = "com.google.android.apps.youtube.kids/9.40.00 (Linux; U; Android 14) gzip",
            clientNameHeaderId = null,
            extraClientFields = mapOf(
                "androidSdkVersion" to 34,
                "osName" to "Android",
                "osVersion" to "14"
            )
        ),
        "ANDROID" to InnerTubeClientProfile_Antigravity(
            clientName = "ANDROID",
            clientVersion = "20.10.38",
            userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip",
            clientNameHeaderId = "3",
            extraClientFields = mapOf(
                "androidSdkVersion" to 34,
                "osName" to "Android",
                "osVersion" to "14"
            )
        ),
        "IOS" to InnerTubeClientProfile_Antigravity(
            clientName = "IOS",
            clientVersion = "20.10.4",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_1 like Mac OS X;)",
            clientNameHeaderId = "5",
            extraClientFields = mapOf(
                "deviceMake" to "Apple",
                "deviceModel" to "iPhone16,2",
                "osName" to "iOS",
                "osVersion" to "18.3.1.22D72"
            )
        ),
        "ANDROID_VR" to InnerTubeClientProfile_Antigravity(
            clientName = "ANDROID_VR",
            clientVersion = "1.62.27",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            clientNameHeaderId = "28",
            extraClientFields = mapOf(
                "deviceMake" to "Oculus",
                "deviceModel" to "Quest 3",
                "androidSdkVersion" to 32,
                "osName" to "Android",
                "osVersion" to "12L"
            )
        ),
        "WEB" to InnerTubeClientProfile_Antigravity(
            clientName = "WEB",
            clientVersion = "2.20240726.00.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            clientNameHeaderId = null,
            extraClientFields = emptyMap()
        )
    )

    fun initialize(context: Context) {
        loadClients(context)
    }

    fun getClient(
        clientName: String,
        context: Context? = null
    ): InnerTubeClientProfile_Antigravity {
        val clients = loadClients(context)
        return clients[clientName]
            ?: defaultClients()[clientName]
            ?: error("Unknown InnerTube client profile: $clientName")
    }

    fun loadClients(context: Context? = null): Map<String, InnerTubeClientProfile_Antigravity> {
        cachedClients?.let { return it }

        val fromDiskOrAsset = if (context != null) {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            val cachedMap = if (cacheFile.exists() && cacheFile.isFile) {
                runCatching { parseConfigJson(cacheFile.readText()) }.getOrNull()
            } else {
                null
            }
            if (!cachedMap.isNullOrEmpty()) {
                cachedMap
            } else {
                runCatching {
                    context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
                        parseConfigJson(reader.readText())
                    }
                }.getOrNull()
            }
        } else {
            null
        }

        val resolved = if (!fromDiskOrAsset.isNullOrEmpty()) {
            defaultClients() + fromDiskOrAsset
        } else {
            defaultClients()
        }
        cachedClients = resolved
        return resolved
    }

    fun parseConfigJson(jsonStr: String): Map<String, InnerTubeClientProfile_Antigravity> {
        val root = JSONObject(jsonStr)
        val clientsObj = root.optJSONObject("clients") ?: return emptyMap()
        val result = linkedMapOf<String, InnerTubeClientProfile_Antigravity>()
        val keys = clientsObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = clientsObj.optJSONObject(key) ?: continue
            val clientName = entry.optString("clientName", key).ifBlank { key }
            val clientVersion = entry.optString("clientVersion").takeIf { it.isNotBlank() } ?: continue
            val userAgent = entry.optString("userAgent").takeIf { it.isNotBlank() } ?: continue
            val headerId = if (entry.isNull("clientNameHeaderId")) {
                null
            } else {
                entry.optString("clientNameHeaderId").takeIf { it.isNotBlank() }
            }

            val extraFields = linkedMapOf<String, Any>()
            val extraObj = entry.optJSONObject("extraClientFields")
            if (extraObj != null) {
                val fieldKeys = extraObj.keys()
                while (fieldKeys.hasNext()) {
                    val fk = fieldKeys.next()
                    val value = extraObj.opt(fk)
                    if (value != null && value !== JSONObject.NULL) {
                        extraFields[fk] = value
                    }
                }
            }

            result[key] = InnerTubeClientProfile_Antigravity(
                clientName = clientName,
                clientVersion = clientVersion,
                userAgent = userAgent,
                clientNameHeaderId = headerId,
                extraClientFields = extraFields
            )
        }
        return result
    }

    suspend fun refreshIfStale(
        context: Context,
        remoteConfigUrl: String? = null
    ): Boolean {
        if (remoteConfigUrl.isNullOrBlank()) return false
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        val now = System.currentTimeMillis()
        if (cacheFile.exists() && (now - cacheFile.lastModified()) < REFRESH_INTERVAL_MS) {
            return false
        }
        val body = InnerTubeHttpClient_Antigravity.getString(remoteConfigUrl) ?: return false
        val parsed = runCatching { parseConfigJson(body) }.getOrNull()
        if (parsed.isNullOrEmpty()) return false

        return runCatching {
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(body)
            if (cacheFile.exists()) cacheFile.delete()
            tmp.renameTo(cacheFile)
            cachedClients = defaultClients() + parsed
            true
        }.getOrDefault(false)
    }
}
