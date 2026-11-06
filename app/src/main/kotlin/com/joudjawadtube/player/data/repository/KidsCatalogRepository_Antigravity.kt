package com.joudjawadtube.player.data.repository

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

data class KidsPlaylistItem(
    val id: String,
    val title: String
)

data class KidsChannelItem(
    val channelId: String,
    val channelName: String,
    val lang: String, // "ar" or "en"
    val ageRange: String,
    val category: String,
    val uploadsPlaylistId: String,
    val playlists: List<KidsPlaylistItem>
)

object KidsCatalogRepository_Antigravity {

    private var cachedChannels: List<KidsChannelItem>? = null

    fun loadKidsChannels(context: Context): List<KidsChannelItem> {
        cachedChannels?.let { return it }

        return try {
            val stream = context.assets.open("kids_catalog_Antigravity.json")
            val jsonText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }
            val list = parseKidsJson(jsonText)
            cachedChannels = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseKidsJson(jsonText: String): List<KidsChannelItem> {
        val list = mutableListOf<KidsChannelItem>()
        val array = JSONArray(jsonText)

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val plArray = obj.getJSONArray("playlists")
            val plList = mutableListOf<KidsPlaylistItem>()
            for (j in 0 until plArray.length()) {
                val plObj = plArray.getJSONObject(j)
                plList.add(
                    KidsPlaylistItem(
                        id = plObj.getString("id"),
                        title = plObj.getString("title")
                    )
                )
            }

            list.add(
                KidsChannelItem(
                    channelId = obj.getString("channelId"),
                    channelName = obj.getString("channelName"),
                    lang = obj.optString("lang", "ar"),
                    ageRange = obj.optString("ageRange", ""),
                    category = obj.optString("category", ""),
                    uploadsPlaylistId = obj.optString("uploadsPlaylistId", ""),
                    playlists = plList
                )
            )
        }
        return list
    }
}
