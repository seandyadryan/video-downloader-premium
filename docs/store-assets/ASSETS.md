# English Play Store upload pack

Copy the text from `short-description-en.txt` and `full-description-en.txt` into Play Console. The export validator checks the 80-character and 4,000-character limits, excluding trailing file line endings.

| Upload field | Files | Specification |
| --- | --- | --- |
| App icon | `play-icon-512.png` | 512 × 512, 32-bit RGBA PNG with an opaque background, under 1 MB |
| Feature graphic | `feature-graphic-1024x500.png` | 1024 × 500, 24-bit RGB PNG without alpha, under 15 MB |
| Phone screenshots | `phone/01-browse.png` through `phone/06-settings.png` | Six real Android screenshots, 1080 × 1920, 9:16, 24-bit RGB PNG without alpha, each under 8 MB |
| Desktop screenshots | `desktop/01-browse.png` through `desktop/06-settings.png` | Six real Android screenshots in a wide emulator window, 1920 × 1080, 16:9, 24-bit RGB PNG without alpha, each under 8 MB |

Screenshots are taken from the English interface by the instrumentation suite. History and tab screenshots use locally rendered example pages titled “Video library” and “Research notes”; these are demonstration entries, not a claim that those websites host downloadable videos. Downloads shows the genuine empty state. No fabricated download progress or connected DNS status is added. Desktop images show the Android application at a wide window size, not a separate Windows/macOS app.

Alt text suggestions: Browse home with website shortcuts; Browser tabs with three open tabs; Device-local browsing history; Download collection; Illustrated download guide; Settings and actual Private DNS status. Use the matching description for both sizes.

The logo was generated using the built-in imagegen tool from the supplied reference. `logo-source.png` preserves that output. `logo-prompt.txt` records the prompt. `tools/Export-StoreArtwork.ps1` exports the padded Android foreground, store icon and feature graphic. `tools/Import-StoreScreenshots.ps1` imports runner images and validates the complete pack. `tools/Package-PlayStoreAssets.ps1` checks asset hashes and creates `artifacts/PlayStore-English-Assets.zip` with the upload files and guide.

Google Play asset reference: https://support.google.com/googleplay/android-developer/answer/9866151
