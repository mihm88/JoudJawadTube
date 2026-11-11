package com.joudjawadtube.player.data.repository

import android.content.Context
import com.joudjawadtube.player.data.settings.AppLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class KidsPlaylistItem(
    val id: String,
    val title: String
)

data class KidsChannelItem(
    val id: String = "",
    val channelId: String,
    val handle: String = "",
    val channelName: String,
    val lang: String, // "ar" or "en"
    val ageRange: String = "",
    val category: String,
    val localizedCategory: String = "",
    val description: String = "",
    val url: String = "",
    val uploadsPlaylistId: String,
    val playlists: List<KidsPlaylistItem>
) {
    val preferredUrl: String
        get() = "https://www.youtube.com/channel/$channelId"
}

object KidsCatalogRepository_Antigravity {

    private val cachedByLocale = mutableMapOf<String, List<KidsChannelItem>>()

    fun isArabicLocale(appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM): Boolean {
        return when (appLanguage) {
            AppLanguage.ARABIC -> true
            AppLanguage.ENGLISH -> false
            AppLanguage.FOLLOW_SYSTEM -> Locale.getDefault().language.equals("ar", ignoreCase = true)
        }
    }

    fun loadKidsChannels(
        context: Context,
        appLanguage: AppLanguage = AppLanguage.FOLLOW_SYSTEM
    ): List<KidsChannelItem> {
        val isAr = isArabicLocale(appLanguage)
        val cacheKey = if (isAr) "ar" else "en"
        cachedByLocale[cacheKey]?.let { return it }

        val assetName = if (isAr) {
            "kids_channels_ar_Antigravity.json"
        } else {
            "kids_channels_en_Antigravity.json"
        }

        return try {
            val stream = runCatching { context.assets.open(assetName) }
                .getOrElse { context.assets.open("kids_catalog_Antigravity.json") }
            val jsonText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            val list = parseKidsJson(jsonText)
            if (list.isNotEmpty()) {
                cachedByLocale[cacheKey] = list
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseKidsJson(jsonText: String): List<KidsChannelItem> {
        val list = mutableListOf<KidsChannelItem>()
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val array = if (trimmed.startsWith("{")) {
            JSONObject(trimmed).getJSONArray("channels")
        } else {
            JSONArray(trimmed)
        }

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val plArray = obj.optJSONArray("playlists")
            val plList = mutableListOf<KidsPlaylistItem>()
            if (plArray != null) {
                for (j in 0 until plArray.length()) {
                    val plObj = plArray.getJSONObject(j)
                    plList.add(
                        KidsPlaylistItem(
                            id = plObj.getString("id"),
                            title = plObj.getString("title")
                        )
                    )
                }
            }

            val cid = obj.getString("channelId")
            val uu = obj.optString("uploadsPlaylistId").ifBlank {
                if (cid.startsWith("UC")) "UU" + cid.substring(2) else ""
            }
            val channelName = obj.optString("channelName").ifBlank { obj.optString("name", cid) }
            val cat = obj.optString("category", "General")

            list.add(
                KidsChannelItem(
                    id = obj.optString("id", cid),
                    channelId = cid,
                    handle = obj.optString("handle", ""),
                    channelName = channelName,
                    lang = obj.optString("lang", "ar"),
                    ageRange = obj.optString("ageRange", ""),
                    category = cat,
                    localizedCategory = obj.optString("localizedCategory", cat),
                    description = obj.optString("description", ""),
                    url = obj.optString("url", ""),
                    uploadsPlaylistId = uu,
                    playlists = plList
                )
            )
        }
        return list
    }
}
