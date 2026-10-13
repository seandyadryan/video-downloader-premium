package com.deploydulupulangnanti.videodownloaderpremium

import java.net.URI
import java.net.URLEncoder
import java.util.Locale

/** Pure rules, shared by the browser and the download queue. */
object MediaRules {
    private val extensions = setOf("mp4", "webm", "m4v", "mov", "mkv", "3gp", "avi")
    fun webUrl(value: String): String? = runCatching {
        val uri = URI(value.trim())
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("https", "http") ||
            uri.host.isNullOrBlank() || uri.rawUserInfo != null ||
            (uri.port != -1 && uri.port !in 1..65535) || value.length > 8192) null
        else uri.toASCIIString().replaceFirst(Regex("^[A-Za-z]+:"), uri.scheme.lowercase(Locale.ROOT) + ":")
    }.getOrNull()
    fun destination(value: String, engine: String): String {
        webUrl(value)?.let { return it }
        if (!value.contains(' ') && value.contains('.') && !value.contains("://") && !value.contains('@')) {
            webUrl("https://$value")?.let { return it }
        }
        val query = URLEncoder.encode(value.trim(), "UTF-8")
        return when (engine) {
            "Google" -> "https://www.google.com/search?q=$query"
            "Bing" -> "https://www.bing.com/search?q=$query"
            else -> "https://duckduckgo.com/?q=$query"
        }
    }
    fun extension(url: String): String = runCatching {
        URI(url).path.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
    }.getOrDefault("")
    fun isVideo(url: String): Boolean = webUrl(url) != null && extension(url) in extensions
    fun canDownload(url: String): Boolean = webUrl(url)?.let {
        URI(it).scheme == "https" && !restrictedHost(URI(it).host)
    } == true
    fun restrictedHost(host: String?): Boolean {
        val h = host.orEmpty().lowercase(Locale.ROOT)
        return listOf("youtube.com", "youtu.be", "googlevideo.com", "youtube-nocookie.com")
            .any { h == it || h.endsWith(".$it") }
    }
    fun fileName(proposed: String, url: String): String {
        val cleaned = proposed.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim().trim('.').ifBlank { "video" }
        val ext = extension(url).takeIf { it in extensions } ?: "mp4"
        val providedExt = cleaned.substringAfterLast('.', "")
        val hasExt = providedExt.lowercase(Locale.ROOT) in extensions
        val suffix = ".${if (hasExt) providedExt else ext}"
        val stem = if (hasExt) cleaned.substringBeforeLast('.') else cleaned
        val result = StringBuilder()
        var bytes = suffix.toByteArray(Charsets.UTF_8).size
        var count = 0
        for (point in stem.codePoints().toArray()) {
            val safePoint = if (point in 0xD800..0xDFFF) '_'.code else point
            val character = String(Character.toChars(safePoint))
            val size = character.toByteArray(Charsets.UTF_8).size
            // Leave room for DownloadStore's unique prefix under the filesystem's byte limit.
            if (bytes + size > 220 || count >= 110) break
            result.append(character); bytes += size; count++
        }
        return result.toString().ifBlank { "video" } + suffix
    }
}
