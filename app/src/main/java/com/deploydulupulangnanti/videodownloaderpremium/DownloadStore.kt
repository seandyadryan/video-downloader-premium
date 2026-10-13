package com.deploydulupulangnanti.videodownloaderpremium

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class DownloadEntry(val id: Long, val url: String, val name: String, val mime: String)
data class DownloadSnapshot(val entry: DownloadEntry, val status: Int, val bytes: Long, val total: Long, val reason: Int)

class DownloadStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)
    val manager: DownloadManager = context.getSystemService(DownloadManager::class.java)
    @Synchronized fun entries(): List<DownloadEntry> = runCatching {
        val array = JSONArray(prefs.getString("entries", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            DownloadEntry(item.getLong("id"), item.getString("url"), item.getString("name"), item.getString("mime"))
        }
    }.getOrDefault(emptyList())
    private fun save(entries: List<DownloadEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().put("id", it.id).put("url", it.url).put("name", it.name).put("mime", it.mime)) }
        prefs.edit().putString("entries", array.toString()).apply()
    }
    @Synchronized fun enqueue(url: String, name: String, mime: String, pageUrl: String?, userAgent: String): Long {
        require(MediaRules.canDownload(url))
        val existing = entries()
        check(existing.size < 500)
        val uniqueName = "${System.currentTimeMillis()}_$name"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(name).setDescription(context.getString(R.string.app_name))
            .setMimeType(mime).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Video Downloader Premium/$uniqueName")
            .addRequestHeader("User-Agent", userAgent)
        val page = pageUrl?.let { MediaRules.webUrl(it) }
        if (page != null && URI(page).scheme == "https" && URI(page).host == URI(url).host) {
            // Never send browser session credentials to a different media host.
            CookieManager.getInstance().getCookie(url)?.takeIf { !it.contains('\r') && !it.contains('\n') }
                ?.let { request.addRequestHeader("Cookie", it) }
            request.addRequestHeader("Referer", page)
        }
        if (context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("wifi", false)) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        }
        val id = manager.enqueue(request)
        save(existing + DownloadEntry(id, url, name, mime))
        return id
    }
    fun snapshots(): List<DownloadSnapshot> = entries().asReversed().map { entry ->
        var result = DownloadSnapshot(entry, 0, 0, -1, 0)
        manager.query(DownloadManager.Query().setFilterById(entry.id))?.use { cursor ->
            if (cursor.moveToFirst()) {
                fun long(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
                result = DownloadSnapshot(entry, long(DownloadManager.COLUMN_STATUS).toInt(),
                    long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR), long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                    long(DownloadManager.COLUMN_REASON).toInt())
            }
        }
        result
    }
    @Synchronized fun remove(id: Long) {
        manager.remove(id)
        save(entries().filterNot { it.id == id })
    }
}
