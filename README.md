# Video Downloader Premium

Native Kotlin Android application for browsing, searching, and saving videos you have permission to download. Paid-once Play Store model, no ads, analytics, subscriptions, backend, or JavaScript application framework.

**Application ID:** `com.deploydulupulangnanti.videodownloaderpremium` · **Android:** 10+ (API 29) · **Target:** Android 16 (API 36) · **Version:** 1.0.0

English Play Store pack: [upload guide](docs/store-assets/ASSETS.md), [short description](docs/store-assets/short-description-en.txt), [full description](docs/store-assets/full-description-en.txt), six phone screenshots and six desktop-size Android screenshots. Native previews: [Browse](docs/store-assets/phone/01-browse.png) · [Tabs](docs/store-assets/phone/02-tabs.png) · [History](docs/store-assets/phone/03-history.png).

## Features

- Integrated HTTPS WebView browser, website shortcut grid, DuckDuckGo/Google/Bing search, navigation and local bookmarks.
- Up to eight independent tabs, local session restoration and a bounded device-local history with titles, URLs, visit times and a clear-history control.
- Direct HTTPS video downloads and discovery of accessible HTML5 video sources / video resource requests. Handles shared links from other apps.
- Persistent Android DownloadManager queue, background downloads, Wi-Fi-only setting, progress, retry, cancellation/removal, open and share completed files.
- Response-header checks before downloading, explicit permission-to-download confirmation, filename sanitization, no DRM or YouTube download bypass.
- Original illustrated four-step tutorial rendered locally with native Canvas; tutorial copy follows the selected language.
- VPN / Private DNS settings for `dns.adguard-dns.com`, with actual **Connected / Unconnected** status from Android network properties.
- 110 offline language/locale packs, language search, device language fallback, Android per-app language support, RTL layouts.
- Light interface, original icons on Browse/Downloads/Guide/Settings, readable cards, system-inset handling, scalable text and local privacy controls.

## Private DNS behavior

Private DNS encrypts DNS queries; it does not hide an IP address or provide a VPN tunnel. An ordinary Android application cannot silently change the system Private DNS hostname. Connect/Disconnect opens the system DNS settings (with network-settings fallback) and copies the exact hostname. The user selects provider hostname to connect, or Automatic/Off to disconnect.

Connected is shown only when the active network has a validated Internet connection, Private DNS is active, and the hostname equals `dns.adguard-dns.com`. It is never a simulated toggle. See [AdGuard public DNS](https://adguard-dns.io/en/public-dns.html) and [Android LinkProperties](https://developer.android.com/reference/android/net/LinkProperties).

## Build locally

Open this folder in Android Studio. Install JDK 17 and Android SDK platform 36 / build tools 36.0.0. Set the SDK location in untracked `local.properties`, or use `ANDROID_HOME`.

```powershell
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Copy `keystore.properties.example` to untracked `keystore.properties` and fill in your local passwords. The provisioned keystore lives at `D:\KEYSTORE\video-downloader-premium.jks`; alias `video-downloader-premium`. Keep a separate offline backup of this key and its credentials.

```powershell
./gradlew.bat assembleRelease bundleRelease
```

The build refuses a release without signing credentials. The release certificate must also match the Play Console fingerprint in `upload-certificate.sha1`; a different keystore is rejected before the build starts. See [upload-key configuration](docs/SIGNING.md).

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/mapping/release/mapping.txt`

Keystore files, credentials, build outputs and SDK paths are ignored by Git. Never place signing passwords in a committed file.

## GitHub Actions runner

`.github/workflows/android.yml` uses GitHub-hosted Ubuntu 24.04 runners. No self-hosted service is installed on the developer computer. All JavaScript-based Actions are pinned to commit SHAs and run on **Node 24**; the Android project itself uses Kotlin/JDK 17 and does not require Node.

On main pushes, the runner validates all translations, executes unit tests and lint, builds a debug APK, runs offline Android emulator tests, then builds/verifies signed release APK and AAB. Pull requests run verification without receiving signing secrets. A live sample-video transfer test is available locally and through the optional run_live_download input when manually dispatching the workflow. Automatic runs use offline device tests to avoid a third-party network dependency.

Required encrypted repository secrets (already provisioned for this repository):

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded JKS contents |
| `KEYSTORE_PASSWORD` | Store password |
| `KEY_PASSWORD` | Key password |
| `KEY_ALIAS` | `video-downloader-premium` |

Retrieve signed artifacts from the successful workflow run. The workflow does not publish an app to Play Console.

## Language coverage

The exact shipped locale list is `app/src/main/assets/languages.txt`. English is the source language. 109 other locale packs contain every translatable UI string. Language coverage includes Indonesian, Arabic, major European and South Asian languages, East Asian languages, and many African and regional languages. This is **110 supported locales**, not every human language. Unlisted languages fall back to English.

Translations are machine-generated drafts and must receive native-speaker review for important launch markets. Indonesian navigation labels receive editorial corrections. No translation service is contacted by the app. The optional maintainer script `tools/Generate-Translations.ps1` sends only source UI copy to the Google Translate public endpoint; that endpoint is not a supported product dependency and may rate-limit/change. Release builds consume the checked-in XML resources without running that script.

```powershell
./tools/Check-Resources.ps1
# Optional maintenance, PowerShell 7 only:
./tools/Generate-Translations.ps1
```

## Tests and limitations

```powershell
./gradlew.bat connectedDebugAndroidTest
```

Device tests cover tab navigation, four tutorial illustrations, actual DNS-status logic, offline locale availability, and a real Android DownloadManager transfer of the [Blender Sintel trailer](https://media.w3.org/2010/05/sintel/trailer.mp4). The network integration test downloads and then deletes its own fixture.

Downloads support ordinary HTTPS video files, not encrypted DRM media, `blob:` URLs, HLS/DASH merging, live streams, YouTube extraction, or sources inaccessible to the browser. Cross-origin frames may not be discoverable. Authenticated servers that reject header or range probes may not be supported. Downloads continue through Android DownloadManager; it manages retries/network waits, so manual pause/resume is not exposed.

Saved files are placed under public Downloads / Video Downloader Premium. Removing an entry also removes its downloaded file. Video playback is delegated to an installed compatible player. Downloads remain on disk after uninstall. Download metadata, including source URLs that may contain tokens, and bookmarks stay in private app storage; Android cloud backup is disabled. No automatic device-wide DNS changes are made.

## Play Store release

See [release preparation](docs/PLAY_STORE.md), [privacy policy draft](docs/PRIVACY_POLICY.md), [terms](docs/TERMS.md), and [store listing draft](docs/STORE_LISTING.md). Play Console pricing, distribution countries, public privacy-policy hosting, contact information, declarations, testing and review are release-owner tasks. A working signed bundle does not guarantee store approval.

This repository is proprietary. See `LICENSE`.
