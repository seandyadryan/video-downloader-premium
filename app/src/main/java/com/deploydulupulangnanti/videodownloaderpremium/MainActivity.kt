package com.deploydulupulangnanti.videodownloaderpremium

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(255, 255, 255)
    private val surface = Color.rgb(243, 245, 249)
    private val green = Color.rgb(20, 123, 86)
    private val textColor = Color.rgb(24, 35, 31)
    private val muted = Color.rgb(112, 127, 142)
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private class BrowserTab(val id: Long) {
        var view: WebView? = null
        @Volatile var url: String? = null
        var title = ""
        var home = true
        var failed = false
        val candidates = Collections.synchronizedMap(linkedMapOf<String, String>())
    }
    private val browserTabs = mutableListOf<BrowserTab>()
    private lateinit var activeTab: BrowserTab
    private lateinit var browserStore: BrowserStore
    private val candidates get() = activeTab.candidates
    private lateinit var content: FrameLayout
    private val browser: WebView get() = requireNotNull(activeTab.view)
    private lateinit var browserLayout: LinearLayout
    private lateinit var address: EditText
    private lateinit var loadProgress: ProgressBar
    private lateinit var store: DownloadStore
    private lateinit var tabs: LinearLayout
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private var currentScreen = "browse"
    private var hasPage: Boolean
        get() = !activeTab.home
        set(value) { activeTab.home = !value; persistTabs() }
    private var resumed = false
    private var dnsStatus: TextView? = null
    private var dnsButton: MaterialButton? = null
    private var downloadCards: LinearLayout? = null
    private var downloadHash = 0
    private var pageUrl: String?
        get() = activeTab.url
        set(value) { activeTab.url = value }
    private var tabCounter: TextView? = null
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { handler.post { updateDns() } }
        override fun onLost(network: Network) { handler.post { updateDns() } }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
            handler.post { updateDns() }
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: android.net.NetworkCapabilities) {
            handler.post { updateDns() }
        }
    }
    private val refreshDownloads = object : Runnable {
        override fun run() {
            if (!resumed || currentScreen != "downloads") return
            io.execute {
                val snapshots = runCatching { store.snapshots() }.getOrDefault(emptyList())
                handler.post {
                    if (resumed && currentScreen == "downloads") {
                        renderDownloads(snapshots)
                        handler.postDelayed(this, 1500)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        store = DownloadStore(this)
        browserStore = BrowserStore(this)
        val root = column().apply { setBackgroundColor(bg) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val header = row().apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(4), dp(18), dp(4)) }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.brand_foreground); contentDescription = getString(R.string.app_name) }, LinearLayout.LayoutParams(dp(44), dp(44)))
        header.addView(label(getString(R.string.brand), 20, textColor, true), LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(label(getString(R.string.premium), 10, green, true).apply {
            background = shape(surface, 8); setPadding(dp(10), dp(6), dp(10), dp(6))
        })
        root.addView(header)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        tabs = row().apply { setPadding(dp(4), dp(6), dp(4), dp(6)); background = shape(surface, 0) }
        root.addView(tabs)
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        restoreTabs(savedInstanceState)
        createBrowserShell()
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getString("screen", "browse")
        }
        showScreen(currentScreen)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentScreen == "browse" && hasPage && browser.canGoBack()) browser.goBack()
                else if (currentScreen != "browse") showScreen("browse")
                else if (hasPage) { hasPage = false; browser.stopLoading(); browser.onPause(); showScreen("browse") }
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        runCatching { getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback) }
        if (!prefs.getBoolean("welcomed", false) && intent.action != Intent.ACTION_SEND) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.welcome_title)
                .setMessage(R.string.welcome_body).setPositiveButton(R.string.start_browsing) { _, _ -> }
                .setNeutralButton(R.string.read_guide) { _, _ -> showScreen("guide") }.show()
            prefs.edit().putBoolean("welcomed", true).apply()
        }
        handleSharedLink(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleSharedLink(intent) }
    private fun handleSharedLink(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().take(8192)
            val link = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(shared)?.value
            if (link != null) { if (MediaRules.isVideo(link)) confirmDownload(link) else navigate(link) }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        persistTabs()
        outState.putString("screen", currentScreen)
        // Keep full active-tab navigation only when it fits safely inside the Binder budget.
        val state = Bundle().also { browser.saveState(it) }
        val parcel = android.os.Parcel.obtain()
        try { parcel.writeBundle(state); if (parcel.dataSize() < 200_000) outState.putBundle("activeWeb", state) }
        finally { parcel.recycle() }
        super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); resumed = true; if (currentScreen == "browse" && hasPage) browser.onResume(); updateDns(); startRefresh() }
    override fun onPause() { resumed = false; handler.removeCallbacks(refreshDownloads); browserTabs.forEach { it.view?.onPause() }; persistTabs(); super.onPause() }
    override fun onDestroy() {
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback) }
        handler.removeCallbacksAndMessages(null); io.shutdown()
        browserTabs.forEach { tab -> tab.view?.let { view -> (view.parent as? ViewGroup)?.removeView(view); view.stopLoading(); view.destroy() } }
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun label(value: String, size: Int = 15, color: Int = textColor, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); gravity = Gravity.CENTER_VERTICAL or Gravity.START
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun button(res: Int, primary: Boolean = false, action: () -> Unit): MaterialButton = MaterialButton(this).apply {
        setText(res); isAllCaps = false; textSize = 13f; minHeight = dp(48)
        cornerRadius = dp(14); insetTop = dp(4); insetBottom = dp(4)
        backgroundTintList = ColorStateList.valueOf(if (primary) green else surface)
        setTextColor(if (primary) bg else green)
        setOnClickListener { action() }
    }
    private fun field(hintRes: Int) = EditText(this).apply {
        hint = getString(hintRes); setTextColor(textColor); setHintTextColor(muted); textSize = 15f
        setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        background = shape(surface, 14); setPadding(dp(16), dp(14), dp(16), dp(14))
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }
    private fun gap(parent: LinearLayout, size: Int = 14) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(size))) }
    private fun page(title: Int, subtitle: Int): LinearLayout {
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val body = column().apply { setPadding(dp(22), dp(20), dp(22), dp(24)) }
        body.addView(label(getString(title), 30, textColor, true)); gap(body, 8)
        body.addView(label(getString(subtitle), 15, muted)); gap(body, 24)
        scroll.addView(body); content.addView(scroll, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp >= 1000) dp(900) else -1, -1, Gravity.CENTER))
        return body
    }
    private fun card(parent: LinearLayout): LinearLayout = column().apply {
        background = shape(surface, 20); setPadding(dp(18), dp(18), dp(18), dp(18))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2)); gap(parent)
    }
    private fun cardText(parent: LinearLayout, title: Int, body: Int) {
        parent.addView(label(getString(title), 18, textColor, true)); gap(parent, 8)
        parent.addView(label(getString(body), 14, muted))
    }
    private fun iconButton(kind: String, title: Int, action: () -> Unit): FrameLayout = FrameLayout(this).apply {
        contentDescription = getString(title); isFocusable = true; isClickable = true
        background = shape(surface, 14)
        addView(UiIcon(this@MainActivity, kind, textColor), FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        setOnClickListener { action() }
        setOnLongClickListener { toast(title); true }
    }
    private fun browserToolbar(): LinearLayout = row().apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(16))
        tabCounter = label(browserTabs.size.toString(), 18, textColor, true).apply {
            gravity = Gravity.CENTER; contentDescription = "${getString(R.string.browser_tabs)}: ${browserTabs.size}"
            background = GradientDrawable().apply { cornerRadius = dp(7).toFloat(); setStroke(dp(2), textColor) }
            setOnClickListener { showScreen("browser_tabs") }; isFocusable = true
        }.also { addView(it, LinearLayout.LayoutParams(dp(44), dp(44))) }
        addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
        listOf(Triple("history", R.string.history, { showScreen("history") }),
            Triple("guide", R.string.guide, { showScreen("guide") }),
            Triple("settings", R.string.settings, { showScreen("settings") }),
            Triple("menu", R.string.browser_menu, { browserMenu() })).forEach { (kind, title, action) ->
            addView(iconButton(kind, title, action), LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(8) })
        }
    }
    private fun showScreen(screen: String) {
        if (currentScreen == "browse") activeTab.view?.onPause()
        currentScreen = screen; handler.removeCallbacks(refreshDownloads)
        content.removeAllViews(); dnsStatus = null; dnsButton = null; downloadCards = null
        tabs.removeAllViews()
        listOf("browse" to R.string.browse, "downloads" to R.string.downloads, "guide" to R.string.guide, "settings" to R.string.settings)
            .forEach { (key, title) ->
                val selected = screen == key || (key == "browse" && screen in listOf("history", "browser_tabs"))
                val tint = if (selected) green else muted
                tabs.addView(column().apply {
                    gravity = Gravity.CENTER; isFocusable = true; contentDescription = getString(title)
                    if (selected) background = shape(Color.rgb(229, 244, 236), 14)
                    addView(UiIcon(this@MainActivity, key, tint), LinearLayout.LayoutParams(dp(24), dp(24)))
                    addView(label(getString(title), 11, tint, selected).apply { gravity = Gravity.CENTER; setOnClickListener { showScreen(key) } })
                    setOnClickListener { showScreen(key) }
                }, LinearLayout.LayoutParams(0, dp(64), 1f))
            }
        when (screen) {
            "browse" -> if (hasPage) attachBrowser() else showHome()
            "downloads" -> showDownloads()
            "guide" -> showGuide()
            "history" -> showHistory()
            "browser_tabs" -> showBrowserTabs()
            else -> showSettings()
        }
    }
    private fun showHome() {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = column()
        body.addView(browserToolbar())
        val inner = column().apply { setPadding(dp(22), dp(12), dp(22), dp(22)) }
        body.addView(inner)
        inner.addView(label(getString(R.string.home_title), 24, textColor, true)); gap(inner, 5)
        inner.addView(label(getString(R.string.home_subtitle), 14, muted)); gap(inner, 22)
        scroll.addView(body); content.addView(scroll, FrameLayout.LayoutParams(if (resources.configuration.screenWidthDp >= 1000) dp(900) else -1, -1, Gravity.CENTER))
        val searchRow = row().apply { gravity = Gravity.CENTER_VERTICAL; background = shape(Color.rgb(241, 240, 250), 22) }
        val input = field(R.string.search_hint)
        input.background = null
        searchRow.addView(input, LinearLayout.LayoutParams(0, dp(60), 1f))
        searchRow.addView(iconButton("search", R.string.go) { navigate(input.text.toString()) }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(6) })
        inner.addView(searchRow); gap(inner, 24)
        input.setOnEditorActionListener { _, _, _ -> navigate(input.text.toString()); true }
        val shortcuts = listOf(
            Triple("Facebook", "https://www.facebook.com", Color.rgb(24,119,242)),
            Triple("Instagram", "https://www.instagram.com", Color.rgb(193,53,132)),
            Triple("Vimeo", "https://vimeo.com", Color.rgb(0,173,239)),
            Triple("Dailymotion", "https://www.dailymotion.com", Color.rgb(0,102,220)),
            Triple("TikTok", "https://www.tiktok.com", Color.rgb(24,24,24)),
            Triple("X", "https://x.com", Color.rgb(24,24,24)),
            Triple("DuckDuckGo", "https://duckduckgo.com", Color.rgb(224,105,50)),
            Triple("WhatsApp", "https://www.whatsapp.com", Color.rgb(37,180,90)))
        val grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        val symbols = listOf("f", "◎", "v", "d", "♪", "X", "D", "W")
        shortcuts.forEachIndexed { index, (name, url, color) ->
            val tile = column().apply {
                gravity = Gravity.CENTER; setPadding(dp(2), dp(8), dp(2), dp(14))
                contentDescription = name; isFocusable = true; setOnClickListener { navigate(url) }
                addView(label(symbols[index], 32, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER; background = shape(color, 40); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(56), dp(56)))
                gap(this, 7); addView(label(name, 11, muted).apply { gravity = Gravity.CENTER; maxLines = 1 })
            }
            grid.addView(tile, GridLayout.LayoutParams().apply { width = 0; height = -2; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) })
        }
        inner.addView(grid, LinearLayout.LayoutParams(-1, -2)); gap(inner, 20)
        val actions = row()
        actions.addView(button(R.string.paste_link) { directLinkDialog() }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(button(R.string.bookmarks) { bookmarksDialog() }, LinearLayout.LayoutParams(0, -2, 1f))
        inner.addView(actions); gap(inner, 18)
        inner.addView(label(getString(R.string.shortcut_note), 12, muted)); gap(inner, 10)
        inner.addView(label(getString(R.string.safe_note), 12, muted))
    }

    private fun persistTabs() {
        if (!::activeTab.isInitialized || !::browserStore.isInitialized) return
        browserStore.saveTabs(browserTabs.map { SavedBrowserTab(it.id, it.url, it.title, it.home) }, activeTab.id)
        tabCounter?.text = browserTabs.size.toString()
    }
    private fun restoreTabs(saved: Bundle?) {
        browserStore.savedTabs().forEach { item -> browserTabs.add(BrowserTab(item.id).apply {
            url = item.url; title = item.title; home = item.home || item.url == null
        }) }
        if (browserTabs.isEmpty()) browserTabs.add(BrowserTab(System.currentTimeMillis()))
        activeTab = browserTabs.firstOrNull { it.id == browserStore.activeId() } ?: browserTabs.first()
        activeTab.view = createWebView(activeTab)
        val restored = saved?.getBundle("activeWeb")?.let { browser.restoreState(it) } != null
        if (!restored && !activeTab.home) activeTab.url?.let { browser.loadUrl(it) }
    }
    private fun newTab() {
        if (browserTabs.size >= BrowserStore.MAX_TABS) { toast(R.string.tab_limit); return }
        browser.onPause()
        val tab = BrowserTab((browserTabs.maxOfOrNull { it.id } ?: System.currentTimeMillis()) + 1)
        browserTabs.add(tab); activeTab = tab; tab.view = createWebView(tab)
        persistTabs(); showScreen("browse")
    }
    private fun selectTab(tab: BrowserTab) {
        browser.onPause(); activeTab = tab
        if (tab.view == null) {
            tab.view = createWebView(tab)
            if (!tab.home) tab.url?.let { requireNotNull(tab.view).loadUrl(it) }
        }
        persistTabs(); showScreen("browse")
    }
    private fun closeTab(tab: BrowserTab) {
        val wasActive = tab === activeTab
        tab.view?.let { view -> (view.parent as? ViewGroup)?.removeView(view); view.stopLoading(); view.destroy() }
        browserTabs.remove(tab)
        if (browserTabs.isEmpty()) {
            val replacement = BrowserTab(System.currentTimeMillis())
            browserTabs.add(replacement); activeTab = replacement; replacement.view = createWebView(replacement)
        } else if (wasActive) {
            activeTab = browserTabs.last()
            if (activeTab.view == null) { activeTab.view = createWebView(activeTab); if (!activeTab.home) activeTab.url?.let { browser.loadUrl(it) } }
        }
        persistTabs(); showScreen("browser_tabs")
    }
    private fun showBrowserTabs() {
        val body = page(R.string.browser_tabs, R.string.tabs_subtitle)
        body.addView(button(R.string.new_tab, true) { newTab() }, LinearLayout.LayoutParams(-1, -2)); gap(body)
        browserTabs.toList().forEachIndexed { index, tab ->
            val tile = card(body)
            val title = if (tab.home) getString(R.string.home) else tab.title.ifBlank { tab.url.orEmpty() }
            tile.addView(label("${index + 1} · $title", 17, if (tab === activeTab) green else textColor, true))
            if (tab === activeTab) tile.addView(label(getString(R.string.active_tab), 12, green))
            if (!tab.home) tile.addView(label(tab.url.orEmpty(), 12, muted).apply { maxLines = 2 })
            val actions = row()
            actions.addView(button(R.string.open) { selectTab(tab) }, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(button(R.string.close_tab) { closeTab(tab) }, LinearLayout.LayoutParams(0, -2, 1f))
            tile.addView(actions)
        }
    }
    private fun showHistory() {
        val body = page(R.string.history, R.string.history_subtitle)
        val items = browserStore.history()
        if (items.isEmpty()) { cardText(card(body), R.string.empty_history, R.string.history_subtitle); return }
        body.addView(button(R.string.clear_history) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.remove) { _, _ -> browserStore.clearHistory(); browserTabs.forEach { it.view?.clearHistory() }; showScreen("history") }
                .setNegativeButton(R.string.cancel, null).show()
        }, LinearLayout.LayoutParams(-1, -2)); gap(body)
        val date = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        items.take(500).forEach { item ->
            val tile = card(body)
            tile.addView(label(item.title, 16, textColor, true).apply { maxLines = 2 })
            tile.addView(label(item.url, 12, muted).apply { maxLines = 2 }); gap(tile, 5)
            tile.addView(label(date.format(java.util.Date(item.time)), 12, muted))
            val actions = row()
            actions.addView(button(R.string.open) { navigate(item.url) }, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(button(R.string.open_new_tab) { if (browserTabs.size < BrowserStore.MAX_TABS) { newTab(); navigate(item.url) } else toast(R.string.tab_limit) }, LinearLayout.LayoutParams(0, -2, 1f))
            tile.addView(actions)
        }
    }
    private fun browserMenu() {
        val options = listOf(R.string.new_tab, R.string.browser_tabs, R.string.history, R.string.bookmarks, R.string.paste_link)
        MaterialAlertDialogBuilder(this).setTitle(R.string.browser_menu).setItems(options.map { getString(it) }.toTypedArray()) { _, which ->
            when (which) { 0 -> newTab(); 1 -> showScreen("browser_tabs"); 2 -> showScreen("history"); 3 -> bookmarksDialog(); 4 -> directLinkDialog() }
        }.setNegativeButton(R.string.close, null).show()
    }
    private fun createBrowserShell() {
        browserLayout = column()
        val searchRow = row().apply { setPadding(dp(10), dp(6), dp(10), dp(6)); gravity = Gravity.CENTER_VERTICAL }
        address = field(R.string.search_hint)
        searchRow.addView(address, LinearLayout.LayoutParams(0, -2, 1f))
        searchRow.addView(button(R.string.go) { navigate(address.text.toString()) })
        address.setOnEditorActionListener { _, _, _ -> navigate(address.text.toString()); true }
        browserLayout.addView(searchRow)
        loadProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = ColorStateList.valueOf(green)
        }
        browserLayout.addView(loadProgress, LinearLayout.LayoutParams(-1, dp(3)))
        val controls = row().apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6)) }
        listOf(Triple("back", R.string.back, { if (browser.canGoBack()) browser.goBack() }),
            Triple("forward", R.string.forward, { if (browser.canGoForward()) browser.goForward() }),
            Triple("reload", R.string.reload, { browser.reload() }),
            Triple("home", R.string.home, { hasPage = false; browser.stopLoading(); showScreen("browse") }),
            Triple("bookmark", R.string.bookmark, { saveBookmark() })).forEach { (kind, title, action) ->
            controls.addView(iconButton(kind, title, action), LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        }
        browserLayout.addView(controls)
        browserLayout.addView(button(R.string.detect, true) { detectVideos() }, LinearLayout.LayoutParams(-1, -2))
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: BrowserTab): WebView {
        val web = WebView(this)
        web.setBackgroundColor(bg)
        web.settings.apply {
            javaScriptEnabled = prefs.getBoolean("js", true); domStorageEnabled = true
            allowFileAccess = false; allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false); javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture = true; builtInZoomControls = true; displayZoomControls = false
            safeBrowsingEnabled = true
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (!request.isForMainFrame) return false
                if (MediaRules.webUrl(url) != null) {
                    if (request.url.scheme == "http") { if (tab === activeTab) toast(R.string.secure_only); view.loadUrl(url.replaceFirst("http:", "https:")); return true }
                    return false
                }
                if (request.url.scheme in setOf("mailto", "tel", "geo")) {
                    MaterialAlertDialogBuilder(this@MainActivity).setMessage(R.string.external_link)
                        .setPositiveButton(R.string.open) { _, _ -> launch(Intent(Intent.ACTION_VIEW, request.url)) }
                        .setNegativeButton(R.string.cancel, null).show()
                }
                return true
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                tab.url = url; tab.failed = false; tab.candidates.clear()
                if (tab === activeTab && ::address.isInitialized) { address.setText(url); loadProgress.visibility = View.VISIBLE }
            }
            override fun onPageFinished(view: WebView, url: String) {
                tab.url = url; tab.title = view.title.orEmpty().ifBlank { url }.take(300)
                if (!tab.failed && !tab.home) browserStore.visit(url, tab.title)
                if (tab === activeTab && ::address.isInitialized) { address.setText(url); loadProgress.visibility = View.GONE }
                persistTabs(); CookieManager.getInstance().flush()
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (request.method == "GET" && MediaRules.isVideo(url) && MediaRules.canDownload(url) &&
                    !MediaRules.restrictedHost(runCatching { URI(tab.url.orEmpty()).host }.getOrNull())) {
                    synchronized(tab.candidates) { if (tab.candidates.size < 100) tab.candidates[url] = "video/${MediaRules.extension(url)}" }
                }
                return null
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                if (request.isForMainFrame) { tab.failed = true; if (tab === activeTab) toast(R.string.browser_error) }
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) tab.failed = true
            }
            override fun onReceivedSslError(view: WebView, sslHandler: SslErrorHandler, error: SslError) {
                tab.failed = true; sslHandler.cancel(); if (tab === activeTab) toast(R.string.browser_error)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) { if (tab === activeTab && ::loadProgress.isInitialized) loadProgress.progress = progress }
        }
        web.setDownloadListener { url, _, _, mime, _ ->
            if (tab !== activeTab) return@setDownloadListener
            if (mime.orEmpty().startsWith("video/") || MediaRules.isVideo(url)) confirmDownload(url, mime = mime)
            else toast(R.string.unsupported)
        }
        web.onPause()
        return web
    }
    private fun attachBrowser() {
        (browserLayout.parent as? ViewGroup)?.removeView(browserLayout)
        // Detach the previous tab without destroying its back/forward list or scroll state.
        for (i in browserLayout.childCount - 1 downTo 0) if (browserLayout.getChildAt(i) is WebView) browserLayout.removeViewAt(i)
        (browser.parent as? ViewGroup)?.removeView(browser)
        browserLayout.addView(browser, 2, LinearLayout.LayoutParams(-1, 0, 1f))
        address.setText(activeTab.url.orEmpty())
        loadProgress.visibility = if (browser.progress < 100) View.VISIBLE else View.GONE
        val wrapper = column()
        wrapper.addView(browserToolbar()); wrapper.addView(browserLayout, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(wrapper, FrameLayout.LayoutParams(-1, -1))
        browser.onResume()
    }
    private fun navigate(value: String) {
        if (value.isBlank()) return
        var url = MediaRules.destination(value, prefs.getString("engine", "DuckDuckGo").orEmpty())
        if (url.startsWith("http:")) { url = url.replaceFirst("http:", "https:"); toast(R.string.secure_only) }
        if (MediaRules.webUrl(url) == null) { toast(R.string.invalid_url); return }
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(content.windowToken, 0)
        hasPage = true; activeTab.url = url; persistTabs(); showScreen("browse"); browser.loadUrl(url)
    }
    private fun directLinkDialog() {
        val input = field(R.string.url_hint)
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.takeIf { MediaRules.webUrl(it) != null }?.let { input.setText(it) }
        MaterialAlertDialogBuilder(this).setTitle(R.string.direct_link).setView(dialogView(input))
            .setPositiveButton(R.string.download) { _, _ -> confirmDownload(input.text.toString().trim()) }
            .setNegativeButton(R.string.cancel, null).show()
    }
    private fun dialogView(child: View) = column().apply { setPadding(dp(22), dp(8), dp(22), dp(8)); addView(child) }
    private fun detectVideos() {
        val sourceTab = activeTab
        if (MediaRules.restrictedHost(runCatching { URI(browser.url.orEmpty()).host }.getOrNull())) { toast(R.string.unsupported); return }
        val script = """
            (function(){return JSON.stringify(Array.from(document.querySelectorAll('video, video source'))
            .map(function(v){return {url:v.currentSrc||v.src, type:v.type||''};})
            .filter(function(v){return v.url && v.url.indexOf('https://')===0;}).slice(0,100));})()
        """.trimIndent()
        browser.evaluateJavascript(script) { encoded ->
            if (sourceTab !== activeTab || sourceTab.home) return@evaluateJavascript
            runCatching {
                val decoded = JSONArray("[$encoded]").getString(0)
                val array = JSONArray(decoded)
                for (i in 0 until array.length()) {
                    val video = array.getJSONObject(i); val url = video.optString("url")
                    if (MediaRules.canDownload(url) && MediaRules.extension(url) !in setOf("m3u8", "mpd")) {
                        candidates[url] = video.optString("type").ifBlank { "video/mp4" }
                    }
                }
            }
            val found = synchronized(candidates) { candidates.toList() }
            if (found.isEmpty()) {
                MaterialAlertDialogBuilder(this).setTitle(R.string.detected).setMessage(R.string.no_media)
                    .setPositiveButton(R.string.paste_link) { _, _ -> directLinkDialog() }.setNegativeButton(R.string.close, null).show()
            } else MaterialAlertDialogBuilder(this).setTitle(R.string.detected)
                .setItems(found.mapIndexed { index, pair -> "${index + 1}. ${MediaRules.fileName(Uri.parse(pair.first).lastPathSegment.orEmpty(), pair.first)}" }.toTypedArray()) { _, which ->
                    confirmDownload(found[which].first, mime = found[which].second)
                }.setNegativeButton(R.string.cancel, null).show()
        }
    }
    private fun confirmDownload(url: String, proposed: String? = null, mime: String? = null) {
        val safe = MediaRules.webUrl(url)
        if (safe == null || !safe.startsWith("https://")) { toast(R.string.invalid_url); return }
        if (!MediaRules.canDownload(safe) || MediaRules.extension(safe) in setOf("m3u8", "mpd")) { toast(R.string.unsupported); return }
        val box = column().apply { setPadding(dp(22), dp(8), dp(22), dp(8)) }
        val input = field(R.string.filename).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(MediaRules.fileName(proposed ?: Uri.parse(safe).lastPathSegment.orEmpty(), safe))
        }
        box.addView(label(getString(R.string.filename), 12, muted)); gap(box, 6); box.addView(input)
        gap(box); box.addView(label(URI(safe).host, 12, muted)); gap(box, 8)
        box.addView(label(getString(R.string.download_location), 12, muted)); gap(box)
        val permission = CheckBox(this).apply { setText(R.string.permission_confirm); setTextColor(textColor); textSize = 14f }
        box.addView(permission)
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.confirm_download).setView(box)
            .setPositiveButton(R.string.download, null).setNegativeButton(R.string.cancel, null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!permission.isChecked) { toast(R.string.permission_required); return@setOnClickListener }
                if (store.entries().size >= 500) { toast(R.string.queue_full); return@setOnClickListener }
                val name = MediaRules.fileName(input.text.toString(), safe)
                // Extensionless URLs are accepted only when found as HTML5 video/downloads with video MIME.
                if (!MediaRules.isVideo(safe) && mime?.startsWith("video/") != true) { toast(R.string.unsupported); return@setOnClickListener }
                val agent = browser.settings.userAgentString
                val sourcePage = pageUrl
                val cookie = CookieManager.getInstance().getCookie(safe)
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                io.execute {
                    val verified = runCatching { VideoProbe.verify(safe, agent, sourcePage, cookie) }
                    handler.post {
                        if (isFinishing || isDestroyed || !dialog.isShowing) return@post
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        verified.onSuccess { resolved ->
                            runCatching { store.enqueue(resolved, name, mime?.takeIf { it.startsWith("video/") } ?: videoMime(resolved), sourcePage, agent) }
                                .onSuccess { toast(R.string.queued); dialog.dismiss(); showScreen("downloads") }
                                .onFailure { toast(R.string.download_error) }
                        }.onFailure { toast(R.string.download_error) }
                    }
                }
            }
        }
        dialog.show()
    }
    private fun videoMime(url: String) = when (MediaRules.extension(url)) {
        "webm" -> "video/webm"; "mov" -> "video/quicktime"; "mkv" -> "video/x-matroska"; "3gp" -> "video/3gpp"
        "avi" -> "video/x-msvideo"; else -> "video/mp4"
    }
    private fun showDownloads() {
        val body = page(R.string.collection_title, R.string.collection_subtitle)
        body.addView(button(R.string.paste_link, true) { directLinkDialog() }, LinearLayout.LayoutParams(-1, -2)); gap(body)
        downloadCards = column().also { body.addView(it) }; downloadHash = Int.MIN_VALUE; startRefresh()
    }
    private fun startRefresh() {
        handler.removeCallbacks(refreshDownloads)
        if (resumed && currentScreen == "downloads") handler.post(refreshDownloads)
    }
    private fun renderDownloads(snapshots: List<DownloadSnapshot>) {
        val parent = downloadCards ?: return
        if (snapshots.hashCode() == downloadHash) return
        downloadHash = snapshots.hashCode(); parent.removeAllViews()
        if (snapshots.isEmpty()) { cardText(card(parent), R.string.empty_downloads, R.string.empty_downloads_body); return }
        snapshots.forEach { item ->
            val tile = card(parent)
            tile.addView(label(item.entry.name, 16, textColor, true))
            gap(tile, 8)
            val state = when (item.status) {
                DownloadManager.STATUS_PENDING -> R.string.pending; DownloadManager.STATUS_RUNNING -> R.string.running
                DownloadManager.STATUS_PAUSED -> R.string.paused; DownloadManager.STATUS_SUCCESSFUL -> R.string.complete
                DownloadManager.STATUS_FAILED -> R.string.failed; else -> R.string.missing
            }
            tile.addView(label(getString(state), 13, if (state == R.string.failed) Color.rgb(255, 172, 163) else green))
            if (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PAUSED || item.status == DownloadManager.STATUS_PENDING) {
                gap(tile, 10)
                tile.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100; isIndeterminate = item.total <= 0
                    if (item.total > 0) progress = (item.bytes * 100 / item.total).toInt().coerceIn(0, 100)
                    progressTintList = ColorStateList.valueOf(green); indeterminateTintList = ColorStateList.valueOf(green)
                }, LinearLayout.LayoutParams(-1, dp(5)))
            }
            gap(tile, 8)
            val size = Formatter.formatFileSize(this, item.bytes) + if (item.total > 0) " / ${Formatter.formatFileSize(this, item.total)}" else ""
            tile.addView(label(size, 12, muted))
            if (item.status == DownloadManager.STATUS_FAILED) tile.addView(label("${getString(R.string.download_reason)}: ${item.reason}", 12, muted))
            val actions = row()
            if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                actions.addView(button(R.string.play) { openDownload(item.entry, false) }, LinearLayout.LayoutParams(0, -2, 1f))
                actions.addView(button(R.string.share) { openDownload(item.entry, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            } else if (item.status == DownloadManager.STATUS_FAILED || item.status == 0) {
                actions.addView(button(R.string.retry) { confirmDownload(item.entry.url, item.entry.name, item.entry.mime) }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            actions.addView(button(R.string.remove) {
                MaterialAlertDialogBuilder(this).setMessage(R.string.delete_download)
                    .setPositiveButton(R.string.remove) { _, _ -> store.remove(item.entry.id); startRefresh() }
                    .setNegativeButton(R.string.cancel, null).show()
            }, LinearLayout.LayoutParams(0, -2, 1f))
            tile.addView(actions)
        }
    }
    private fun openDownload(entry: DownloadEntry, share: Boolean) {
        val uri = runCatching { store.manager.getUriForDownloadedFile(entry.id) }.getOrNull()
        if (uri == null) { toast(R.string.missing); return }
        val intent = if (share) Intent(Intent.ACTION_SEND).apply {
            type = entry.mime; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri(entry.name, uri)
        } else Intent(Intent.ACTION_VIEW).setDataAndType(uri, entry.mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launch(if (share) Intent.createChooser(intent, getString(R.string.share)) else intent)
    }
    private fun showGuide() {
        val body = page(R.string.guide_title, R.string.guide_subtitle)
        val titles = listOf(R.string.step_one_title, R.string.step_two_title, R.string.step_three_title, R.string.step_four_title)
        val copies = listOf(R.string.step_one_body, R.string.step_two_body, R.string.step_three_body, R.string.step_four_body)
        for (index in 0..3) {
            val tile = card(body)
            tile.addView(TutorialImage(this, index + 1), LinearLayout.LayoutParams(-1, dp(170)))
            gap(tile); tile.addView(label("0${index + 1}", 12, green, true)); gap(tile, 6)
            cardText(tile, titles[index], copies[index])
        }
        body.addView(label(getString(R.string.safe_note), 12, muted)); gap(body)
        body.addView(button(R.string.try_now, true) { showScreen("browse") }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun showSettings() {
        val body = page(R.string.preferences, R.string.preferences_subtitle)
        val dns = card(body)
        dns.addView(label(getString(R.string.dns_title), 18, textColor, true)); gap(dns, 10)
        dnsStatus = label("", 14, green, true).also { dns.addView(it) }
        gap(dns, 8); dns.addView(label(PrivateDns.HOST, 14, textColor)); gap(dns, 10)
        dns.addView(label(getString(R.string.dns_explanation), 13, muted)); gap(dns)
        dns.addView(label(getString(R.string.dns_instructions), 13, muted)); gap(dns)
        val dnsActions = row()
        dnsButton = button(R.string.connect, true) { openDnsSettings() }.also { dnsActions.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
        dnsActions.addView(button(R.string.copy_hostname) {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Private DNS", PrivateDns.HOST)); toast(R.string.copied)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        dns.addView(dnsActions); updateDns()
        val general = card(body)
        general.addView(button(R.string.language) { languageDialog() }, LinearLayout.LayoutParams(-1, -2))
        general.addView(label(getString(R.string.language_note), 12, muted)); gap(general)
        general.addView(button(R.string.search_engine) {
            val engines = arrayOf("DuckDuckGo", "Google", "Bing")
            MaterialAlertDialogBuilder(this).setTitle(R.string.search_engine)
                .setSingleChoiceItems(engines, engines.indexOf(prefs.getString("engine", "DuckDuckGo"))) { dialog, which ->
                    prefs.edit().putString("engine", engines[which]).apply(); dialog.dismiss()
                }.setNegativeButton(R.string.cancel, null).show()
        }, LinearLayout.LayoutParams(-1, -2))
        toggle(general, R.string.wifi_only, R.string.wifi_body, "wifi", false) {}
        toggle(general, R.string.javascript, R.string.javascript_body, "js", true) { enabled -> browserTabs.forEach { it.view?.settings?.javaScriptEnabled = enabled } }
        val privacy = card(body); cardText(privacy, R.string.privacy, R.string.privacy_body); gap(privacy)
        privacy.addView(button(R.string.clear_browser) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.clear_browser_confirm)
                .setPositiveButton(R.string.remove) { _, _ ->
                    browserTabs.forEach { tab ->
                        tab.home = true; tab.url = null; tab.title = ""; tab.candidates.clear()
                        tab.view?.let { view -> view.stopLoading(); view.loadUrl("about:blank"); view.clearCache(true); view.clearHistory() }
                    }
                    browserStore.clearHistory(); persistTabs()
                    WebStorage.getInstance().deleteAllData(); CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
                    toast(R.string.saved)
                }.setNegativeButton(R.string.cancel, null).show()
        }, LinearLayout.LayoutParams(-1, -2))
        cardText(card(body), R.string.terms, R.string.terms_body)
        val about = card(body); cardText(about, R.string.about, R.string.paid_app); gap(about, 10)
        about.addView(label("${getString(R.string.version)} ${BuildConfig.VERSION_NAME}", 12, muted))
    }
    private fun toggle(parent: LinearLayout, title: Int, copy: Int, key: String, initial: Boolean, change: (Boolean) -> Unit) {
        gap(parent)
        parent.addView(SwitchMaterial(this).apply {
            setText(title); setTextColor(textColor); textSize = 14f; isChecked = prefs.getBoolean(key, initial)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply(); change(checked) }
        }, LinearLayout.LayoutParams(-1, -2))
        parent.addView(label(getString(copy), 12, muted))
    }
    private fun updateDns() {
        val connected = runCatching { PrivateDns.connected(this) }.getOrDefault(false)
        dnsStatus?.setText(if (connected) R.string.connected else R.string.unconnected)
        dnsButton?.setText(if (connected) R.string.disconnect else R.string.connect)
    }
    private fun openDnsSettings() {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Private DNS", PrivateDns.HOST))
        val opened = runCatching { startActivity(Intent("android.settings.PRIVATE_DNS_SETTINGS")) }.isSuccess
        if (!opened) launch(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }
    private fun languageDialog() {
        val tags = assets.open("languages.txt").bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        val languages = tags.map { tag ->
            val locale = Locale.forLanguageTag(tag)
            tag to "${locale.getDisplayName(locale)} · ${locale.getDisplayName(Locale.ENGLISH)}"
        }.sortedBy { it.second }
        val options = listOf("" to getString(R.string.system_language)) + languages
        val box = column().apply { setPadding(dp(20), dp(8), dp(20), 0) }
        val search = field(R.string.search_language).apply { inputType = android.text.InputType.TYPE_CLASS_TEXT }
        box.addView(search); gap(box)
        val list = ListView(this)
        val shown = options.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, shown.map { it.second }.toMutableList())
        list.adapter = adapter; box.addView(list, LinearLayout.LayoutParams(-1, dp(330)))
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.language).setView(box).setNegativeButton(R.string.cancel, null).create()
        list.setOnItemClickListener { _, _, position, _ ->
            val tag = shown[position].first; dialog.dismiss()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                shown.clear(); shown.addAll(options.filter { it.second.contains(query, ignoreCase = true) || it.first.contains(query, ignoreCase = true) })
                adapter.clear(); adapter.addAll(shown.map { it.second }); adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        dialog.show()
    }
    private fun bookmarkEntries(): List<Pair<String, String>> = runCatching {
        val array = JSONArray(prefs.getString("bookmarks", "[]"))
        (0 until array.length()).map { array.getJSONObject(it).let { item -> item.getString("url") to item.getString("title") } }
    }.getOrDefault(emptyList())
    private fun saveBookmark() {
        val url = browser.url?.let { MediaRules.webUrl(it) } ?: return
        val items = bookmarkEntries().filterNot { it.first == url }.takeLast(199) + (url to browser.title.orEmpty().ifBlank { url })
        saveBookmarks(items); toast(R.string.saved)
    }
    private fun saveBookmarks(items: List<Pair<String, String>>) {
        val array = JSONArray(); items.forEach { array.put(JSONObject().put("url", it.first).put("title", it.second)) }
        prefs.edit().putString("bookmarks", array.toString()).apply()
    }
    private fun bookmarksDialog() {
        val items = bookmarkEntries()
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.bookmarks).setNegativeButton(R.string.close, null)
        if (items.isEmpty()) dialog.setMessage(R.string.empty_bookmarks)
        else dialog.setItems(items.map { it.second }.toTypedArray()) { _, which ->
            val item = items[which]
            MaterialAlertDialogBuilder(this).setTitle(item.second).setMessage(item.first)
                .setPositiveButton(R.string.open) { _, _ -> navigate(item.first) }
                .setNeutralButton(R.string.remove) { _, _ -> saveBookmarks(items.filterNot { it.first == item.first }) }
                .setNegativeButton(R.string.cancel, null).show()
        }
        dialog.show()
    }
    private fun launch(intent: Intent) { runCatching { startActivity(intent) }.onFailure { toast(R.string.no_player) } }
    private fun toast(res: Int) { if (!isFinishing && !isDestroyed) Toast.makeText(this, res, Toast.LENGTH_LONG).show() }
}
