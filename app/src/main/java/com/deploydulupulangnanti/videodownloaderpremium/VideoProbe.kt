package com.deploydulupulangnanti.videodownloaderpremium

import java.net.HttpURLConnection
import java.net.URI

/** Check response headers before handing a URL to Android; never buffer the video. */
object VideoProbe {
    fun verify(url: String, userAgent: String, pageUrl: String? = null, cookie: String? = null): String {
        var current = url
        var method = "HEAD"
        repeat(8) {
            require(MediaRules.canDownload(current))
            val connection = URI(current).toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = method
                connection.connectTimeout = 10000; connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", userAgent)
                val page = pageUrl?.let { MediaRules.webUrl(it) }
                if (page != null && URI(page).scheme == "https" && URI(page).host == URI(current).host && URI(url).host == URI(current).host) {
                    connection.setRequestProperty("Referer", page)
                    cookie?.takeIf { !it.contains('\r') && !it.contains('\n') }?.let { connection.setRequestProperty("Cookie", it) }
                }
                if (method == "GET") connection.setRequestProperty("Range", "bytes=0-0")
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    current = URI(current).resolve(connection.getHeaderField("Location") ?: error("No redirect target")).toString()
                } else if (method == "HEAD" && code in listOf(403, 405, 501)) {
                    check(method != "GET"); method = "GET"
                } else {
                    check(code in 200..299)
                    val mime = connection.contentType.orEmpty().substringBefore(';').lowercase(java.util.Locale.ROOT)
                    require(mime.startsWith("video/") || (MediaRules.isVideo(current) && mime in listOf("application/octet-stream", "binary/octet-stream", "")))
                    return current
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
}
