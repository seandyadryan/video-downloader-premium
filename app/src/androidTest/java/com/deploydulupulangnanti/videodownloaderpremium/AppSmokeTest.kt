package com.deploydulupulangnanti.videodownloaderpremium

import android.app.DownloadManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    @Before fun skipOnboarding() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("welcomed", true).putBoolean("wifi", false).commit()
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().clear().commit()
        instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
    }
    private fun all(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
    private fun click(activity: MainActivity, text: String) {
        all(activity.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == text }.performClick()
    }
    private fun screenshot(name: String) {
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse").close()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(1000)
        val deadline = System.currentTimeMillis() + 5000
        var foreground = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
        while (foreground != context.packageName && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            foreground = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
        }
        assertEquals("Screenshot must show the application, not a system overlay", context.packageName, foreground)
        val image = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        // Public test images survive AGP uninstalling the test target after the suite.
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VideoDownloaderPremium-QA")
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
            values.clear(); values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } finally { image.recycle() }
    }
    @Test fun navigationGuideAndRealDnsStatus() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                click(activity, activity.getString(R.string.guide))
                assertEquals(4, all(activity.window.decorView).filterIsInstance<TutorialImage>().size)
            }
            screenshot("guide")
            scenario.onActivity { activity ->
                click(activity, activity.getString(R.string.settings))
                val expected = activity.getString(if (PrivateDns.connected(activity)) R.string.connected else R.string.unconnected)
                assertTrue(all(activity.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == expected })
            }
            screenshot("settings")
            scenario.onActivity { activity ->
                click(activity, activity.getString(R.string.downloads))
                click(activity, activity.getString(R.string.browse))
                assertTrue(all(activity.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == activity.getString(R.string.home_title) })
            }
            screenshot("home")
        }
    }
    @Test fun languagePacksAreCompleteAndOffline() {
        val tags = context.assets.open("languages.txt").bufferedReader().use { it.readLines() }
        assertTrue(tags.size >= 100)
        tags.forEach { tag ->
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(java.util.Locale.forLanguageTag(tag))
            val resources = context.createConfigurationContext(config).resources
            assertTrue(resources.getString(R.string.step_three_body).isNotBlank())
            assertTrue(resources.getString(R.string.dns_explanation).isNotBlank())
        }
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag("id"))
        assertNotEquals(context.getString(R.string.browse), context.createConfigurationContext(config).getString(R.string.browse))
    }
    @Test fun actualDownloadManagerTransfer() {
        val store = DownloadStore(context)
        // Blender Open Movie sample, used only as a network integration fixture.
        val url = "https://media.w3.org/2010/05/sintel/trailer.mp4"
        val resolved = VideoProbe.verify(url, "VideoDownloaderPremium-Test")
        val id = store.enqueue(resolved, "integration-test.mp4", "video/mp4", null, "VideoDownloaderPremium-Test")
        try {
            val deadline = System.currentTimeMillis() + 90000
            var state = store.snapshots().first { it.entry.id == id }
            while (state.status != DownloadManager.STATUS_SUCCESSFUL && state.status != DownloadManager.STATUS_FAILED && System.currentTimeMillis() < deadline) {
                Thread.sleep(500); state = store.snapshots().first { it.entry.id == id }
            }
            assertEquals("Download failed, reason ${state.reason}", DownloadManager.STATUS_SUCCESSFUL, state.status)
            assertTrue(state.bytes > 0); assertNotNull(store.manager.getUriForDownloadedFile(id))
        } finally { store.remove(id) }
    }
    private fun invoke(activity: MainActivity, method: String, vararg args: String) {
        val parameterTypes = args.map { String::class.java }.toTypedArray()
        MainActivity::class.java.getDeclaredMethod(method, *parameterTypes).apply { isAccessible = true }.invoke(activity, *args)
    }
    private fun fixture(scenario: ActivityScenario<MainActivity>, url: String, title: String) {
        scenario.onActivity { activity ->
            invoke(activity, "navigate", url)
            val web = all(activity.window.decorView).filterIsInstance<WebView>().single()
            web.loadDataWithBaseURL(url, "<html><head><title>$title</title></head><body><h1>$title</h1></body></html>", "text/html", "UTF-8", url)
        }
        val deadline = System.currentTimeMillis() + 10000
        while (BrowserStore(context).history().none { it.url == url && it.title == title } && System.currentTimeMillis() < deadline) Thread.sleep(100)
        assertTrue(BrowserStore(context).history().any { it.url == url && it.title == title })
    }
    @Test fun browserTabsHistoryAndRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fixture(scenario, "https://example.com/library", "Video library")
            scenario.onActivity { invoke(it, "newTab") }
            fixture(scenario, "https://example.org/research", "Research notes")
            assertEquals(2, BrowserStore(context).savedTabs().size)
            val firstId = BrowserStore(context).savedTabs().first().id
            scenario.onActivity { activity ->
                invoke(activity, "showScreen", "browser_tabs")
                all(activity.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == activity.getString(R.string.open) }.performClick()
                assertEquals("Video library", all(activity.window.decorView).filterIsInstance<WebView>().single().title)
            }
            assertEquals(firstId, BrowserStore(context).activeId())
            scenario.recreate()
            assertEquals(2, BrowserStore(context).savedTabs().size)
            assertEquals(firstId, BrowserStore(context).activeId())
            scenario.onActivity { activity ->
                repeat(9) { invoke(activity, "newTab") }
                assertEquals(8, BrowserStore(activity).savedTabs().size)
                invoke(activity, "showScreen", "browser_tabs")
                repeat(8) { click(activity, activity.getString(R.string.close_tab)) }
                assertEquals(1, BrowserStore(activity).savedTabs().size)
                invoke(activity, "showScreen", "history")
                assertTrue(all(activity.window.decorView).filterIsInstance<TextView>().any { it.text.toString() == "Video library" })
                click(activity, activity.getString(R.string.clear_history))
            }
            instrumentation.waitForIdleSync()
            // Query accessibility from the test thread: the app UI thread must remain free to answer.
            val deadline = System.currentTimeMillis() + 5000
            var nodes = instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(context.getString(R.string.remove)).orEmpty()
            while (nodes.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
                nodes = instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(context.getString(R.string.remove)).orEmpty()
            }
            val node = nodes.first()
            assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            assertTrue(BrowserStore(context).history().isEmpty())
        }
    }
    @Test fun storeScreenshots() {
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: "phone"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            screenshot("$prefix-01-browse")
            fixture(scenario, "https://example.com/library", "Video library")
            scenario.onActivity { invoke(it, "newTab") }
            fixture(scenario, "https://example.org/research", "Research notes")
            scenario.onActivity { invoke(it, "newTab"); invoke(it, "showScreen", "browser_tabs") }
            screenshot("$prefix-02-tabs")
            scenario.onActivity { invoke(it, "showScreen", "history") }
            screenshot("$prefix-03-history")
            scenario.onActivity { invoke(it, "showScreen", "downloads") }
            screenshot("$prefix-04-downloads")
            scenario.onActivity { invoke(it, "showScreen", "guide") }
            screenshot("$prefix-05-guide")
            scenario.onActivity { invoke(it, "showScreen", "settings") }
            screenshot("$prefix-06-settings")
        }
    }
}
