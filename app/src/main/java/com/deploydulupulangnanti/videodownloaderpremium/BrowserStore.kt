package com.deploydulupulangnanti.videodownloaderpremium

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BrowserVisit(val url: String, val title: String, val time: Long)
data class SavedBrowserTab(val id: Long, val url: String?, val title: String, val home: Boolean)

/** Bounded, device-local browser metadata. No page HTML, cookies or credentials are stored here. */
class BrowserStore(context: Context) {
    private val prefs = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    fun history(): List<BrowserVisit> = runCatching {
        val array = JSONArray(prefs.getString("history", "[]"))
        (0 until array.length()).map { array.getJSONObject(it).let { item ->
            BrowserVisit(item.getString("url"), item.getString("title"), item.getLong("time"))
        } }.filter { MediaRules.webUrl(it.url) != null }.take(500)
    }.getOrDefault(emptyList())
    fun visit(url: String, title: String) {
        if (MediaRules.webUrl(url) == null) return
        val items = listOf(BrowserVisit(url.take(8192), title.take(300).ifBlank { url }, System.currentTimeMillis())) +
            history().filterNot { it.url == url }.take(499)
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("url", it.url).put("title", it.title).put("time", it.time)) }
        prefs.edit().putString("history", array.toString()).apply()
    }
    fun clearHistory() { prefs.edit().remove("history").apply() }
    fun savedTabs(): List<SavedBrowserTab> = runCatching {
        val array = JSONArray(prefs.getString("tabs", "[]"))
        (0 until array.length()).map { array.getJSONObject(it).let { item ->
            SavedBrowserTab(item.getLong("id"), item.optString("url").takeIf { MediaRules.webUrl(it) != null },
                item.optString("title").take(300), item.optBoolean("home", true))
        } }.distinctBy { it.id }.take(MAX_TABS)
    }.getOrDefault(emptyList())
    fun activeId(): Long = prefs.getLong("active", -1)
    fun saveTabs(tabs: List<SavedBrowserTab>, active: Long) {
        val array = JSONArray()
        tabs.take(MAX_TABS).forEach { tab -> array.put(JSONObject().put("id", tab.id).put("url", tab.url.orEmpty())
            .put("title", tab.title.take(300)).put("home", tab.home)) }
        prefs.edit().putString("tabs", array.toString()).putLong("active", active).apply()
    }
    companion object { const val MAX_TABS = 8 }
}
