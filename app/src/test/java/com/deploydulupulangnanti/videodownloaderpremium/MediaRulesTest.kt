package com.deploydulupulangnanti.videodownloaderpremium

import org.junit.Assert.*
import org.junit.Test

class MediaRulesTest {
    @Test fun rejectUntrustedSchemesAndCredentials() {
        listOf("javascript:alert(1)", "file:///sdcard/video.mp4", "content://downloads/12", "data:text/html,test",
            "https://user:pass@example.com/file.mp4", "https://example.com:99999/x", "https:///file").forEach { assertNull(MediaRules.webUrl(it)) }
    }
    @Test fun allowSignedHttpsMedia() {
        val signed = "https://cdn.example.com/video.MP4?signature=a%2Fb&expires=123"
        assertEquals(signed, MediaRules.webUrl(signed)); assertTrue(MediaRules.isVideo(signed)); assertTrue(MediaRules.canDownload(signed))
        assertFalse(MediaRules.canDownload("http://example.com/video.mp4"))
        assertTrue(MediaRules.canDownload("HTTPS://example.com/video.mp4"))
    }
    @Test fun preventFilenameTraversalAndControlCharacters() {
        val name = MediaRules.fileName("../../a\\b:bad\nfile", "https://example.com/media.webm?token=secret")
        assertFalse(name.contains('/')); assertFalse(name.contains('\\')); assertFalse(name.contains('\n'))
        assertTrue(name.endsWith(".webm")); assertEquals("video.mp4", MediaRules.fileName("...", "https://example.com/file"))
    }
    @Test fun searchEncodingAndWebsiteNavigation() {
        assertEquals("https://example.com/video", MediaRules.destination("example.com/video", "DuckDuckGo"))
        assertEquals("https://duckduckgo.com/?q=cats+%26+dogs", MediaRules.destination("cats & dogs", "DuckDuckGo"))
        assertTrue(MediaRules.destination("a@b.com", "Bing").startsWith("https://www.bing.com/search"))
    }
    @Test fun restrictHostsWithDomainBoundaries() {
        assertFalse(MediaRules.canDownload("https://rr1.googlevideo.com/video.mp4"))
        assertFalse(MediaRules.canDownload("https://www.youtube.com/video.mp4"))
        assertTrue(MediaRules.canDownload("https://notyoutube.com/video.mp4"))
    }
    @Test fun identifyOnlyFilesRatherThanPlaylists() {
        assertFalse(MediaRules.isVideo("https://example.com/video.m3u8"))
        assertFalse(MediaRules.isVideo("https://example.com/page?file=video.mp4"))
        assertFalse(MediaRules.isVideo("blob:https://example.com/12"))
    }
    @Test fun unicodeNamesFitTheFilesystemWithoutSplittingCharacters() {
        val name = MediaRules.fileName("旅".repeat(150) + "🎬".repeat(100), "https://example.com/video.mp4")
        assertTrue(name.toByteArray(Charsets.UTF_8).size <= 220)
        assertTrue(name.endsWith(".mp4"))
        val emoji = MediaRules.fileName("🎬".repeat(100), "https://example.com/video.webm")
        assertTrue(emoji.toByteArray(Charsets.UTF_8).size <= 220)
        assertFalse(emoji.contains('\uFFFD'))
        assertEquals(emoji, String(emoji.toByteArray(Charsets.UTF_8), Charsets.UTF_8))
    }
}
