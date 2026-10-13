# Commercial release preparation

The application is implemented as a **paid-once app**. Configure a paid listing and country prices in Google Play Console before first publication; the source does not use in-app purchases or subscriptions. Consult the current [Google Play payments policy](https://support.google.com/googleplay/android-developer/answer/9858738).

## Project configuration

| Field | Value |
| --- | --- |
| Application ID | `com.deploydulupulangnanti.videodownloaderpremium` |
| Launch version | 1 / 1.0.0 |
| Minimum Android | 10 / API 29 |
| Target Android | 16 / API 36 |
| Release artifact | Signed `app-release.aab` |
| Signing alias | `video-downloader-premium` |
| DNS provider | `dns.adguard-dns.com` |
| Monetization | Upfront paid Play Store app |

Android 16 target API 36 meets the published new-app requirement effective August 31, 2026. Recheck the [official target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) at submission.

## Before submission

- Complete Play developer account, identity/merchant verification, bank/tax details, and applicable testing requirements.
- Enroll in Play App Signing; use the supplied JKS as the upload key as appropriate to your chosen signing configuration. Back it up securely. Never publish the keystore or passwords.
- Replace all publisher and support placeholders in the privacy policy. Host the final policy at a public HTTPS URL and set the URL in Play Console.
- Configure the paid price, currency/country distribution, contact email, category, content rating and audience.
- Complete Data Safety based on actual behavior and Google's current definitions. Website/search-provider processing still needs evaluation; do not blindly mark every declaration as no data.
- Declare ads accurately: this app contains no developer ads/advertising SDK. Third-party pages can display their own content.
- The original 512×512 icon and 1024×500 feature graphic are supplied in `docs/store-assets`. Add final release screenshots. Do not claim VPN tunneling, hidden IPs, universal website support, DRM bypass or every human language.
- Review machine translations with speakers of each important market language. The app includes 110 locale packs, but translation quality has not been professionally certified.
- Install the signed APK on physical devices, test download errors, network switching/Wi-Fi-only waits, Private DNS connect/disconnect and offline playback through external players.
- Upload the signed AAB to an internal track, review Play pre-launch reports, satisfy any account-specific closed testing, then submit for review.
- Increment `versionCode` for every subsequent uploaded bundle. Keep the same application ID and signing arrangement.

The project does not have Play Console credentials and does not automatically publish to the store. Google Play review may require changes; approval is not guaranteed.
