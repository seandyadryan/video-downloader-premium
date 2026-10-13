# Validation record

13 October 2026, local Windows build with JDK 17, Gradle 8.14, AGP 8.13.1 and Kotlin 2.2.20.

| Check | Result |
| --- | --- |
| Complete offline locale packs | 110 locales; 126 translated strings per non-English pack |
| Kotlin unit tests | 7 passed, 0 failed |
| Debug Android lint | 0 errors; 44 advisory warnings after the browser/tab update |
| Release lint | Passed |
| Debug APK | Built |
| Android instrumentation APK | Built |
| Minified signed release APK | Built; APK Signature Scheme v2 verified |
| Signed Play Store AAB | Built; JAR signature verified |
| Package metadata | Exact requested application ID, version 1 / 1.0.0, min 29, target 36 |
| Initial generated certificate SHA-256 (superseded for Play uploads) | `4b436930415a49260d7e457afdb76e422aebae628718bf9fbf2a5b07ad0edafc` |
| Original store artwork | PNG 512×512 icon; PNG 1024×500 feature graphic; visually inspected |

Unit tests cover unsafe schemes and credentials, signed video URLs, HTTPS scheme normalization, filename traversal/control characters and UTF-8 byte limits, search encoding, restricted host boundaries and playlist/blob rejection. Lint advisories include newer dependency versions, KTX style suggestions, static tutorial draw allocations, redundant SDK qualifiers and translation spelling hints; they are not build failures.

The Android test suite covers navigation, tutorial image presence, DNS status presentation, all offline locale packs, independent browser tabs, activity recreation, tab limits, closing all tabs, clearing history and a real network download fixture. The CI device job runs three offline tests on an API 35 Pixel 6 emulator, then captures six English phone screenshots at 1080×1920 and six desktop-size Android screenshots at 1920×1080. The public sample-video network transfer is available through the optional manual run_live_download input. See the repository Actions run for the actual CI result.

The local Windows hypervisor emulator did not finish boot during the initial build verification; software emulation was attempted. Physical-device behavior, professionally reviewed translations, region-specific store declarations and Play review remain release-owner acceptance checks.

Signing files, credentials and local artifacts are excluded from Git. Encrypted repository secrets supply signing only to trusted main/tag workflow runs; fork pull requests do not receive the signing job.

## Browser update and English store pack — 14 October 2026

[Verified CI run](https://github.com/seandyadryan/video-downloader-premium/actions/runs/38509437198): all three jobs passed. Seven Kotlin unit tests and three offline device tests passed; English store captures also passed at both phone and desktop window sizes. The browser test checks switching independent pages, preserving the active tab across activity recreation, the eight-tab limit, closing all tabs to a fresh Home tab, and deleting history through its confirmation dialog.

All twelve final screenshots were visually inspected after capture. Each is a 24-bit RGB PNG without alpha and below 8 MB: six phone PNGs at 1080×1920 (9:16) and six desktop-size Android PNGs at 1920×1080 (16:9). The feature graphic is an opaque 24-bit 1024×500 PNG below 15 MB. The store icon is a 32-bit RGBA 512×512 PNG below 1 MB. PNG IHDR checks validate these color formats. English copy is 77/80 characters for the short description and 2,456/4,000 for the full description. `asset-manifest.json` records dimensions, color depth, byte counts and SHA-256 hashes; the packaging script checks those hashes before making the upload ZIP. Earlier captures containing the notification shade were replaced and are not part of the final pack.

The signed release APK and AAB were downloaded from that run and their SHA-256 hashes matched its checksum file. Screenshot fixture entries are documented in the upload guide. The final screenshot files and manifest are documentation assets; they do not change the verified application source.

The documentation/assets commit was also verified by [CI run 38510607119](https://github.com/seandyadryan/video-downloader-premium/actions/runs/38510607119), with all three jobs successful. The final store-icon color-depth export and local asset validator were checked with both export/import scripts and the ZIP hash checks; these do not change Android application resources or code.

## Play upload-key correction — 14 October 2026

The initially generated upload key was rejected by Play Console because its SHA-1 (`45:95:7D:AD:C3:92:4F:DF:C0:16:31:A9:73:F7:95:35:04:36:F3:6A`) differs from the existing registered upload certificate. The matching private key was found locally, imported into the project's JKS without changing the source keystore, and verified against expected SHA-1 `6B:2A:50:FC:22:84:89:4B:78:A5:AB:2D:5D:74:79:55:CF:FF:F1:E2`. The previous project key was backed up. GitHub's signing-keystore secret now uses this matching JKS.

Gradle release-graph validation was exercised with both files: the old certificate caused a nonzero build exit with “Wrong release upload key”; the matching certificate passed the release dry run. See [signing details and public certificate](SIGNING.md). The application ID and version remain unchanged.
