# Google Play upload key

Play Console expects upload certificate SHA-1 `6B:2A:50:FC:22:84:89:4B:78:A5:AB:2D:5D:74:79:55:CF:FF:F1:E2`.

The matching private key was found in the owner's existing `D:\KEYSTORE\Personal-asset-Passport.jks`, alias `key`. It was imported into the project JKS using alias `video-downloader-premium`. The source keystore was preserved. The project's previous generated key was copied to `D:\KEYSTORE\video-downloader-premium-original-45957dad.jks` before replacement.

Active project keystore: `D:\KEYSTORE\video-downloader-premium.jks`. Existing local signing passwords remain in ignored `keystore.properties`; private key files and passwords must never be committed.

| Fingerprint | Correct upload certificate |
| --- | --- |
| SHA-1 | `6B:2A:50:FC:22:84:89:4B:78:A5:AB:2D:5D:74:79:55:CF:FF:F1:E2` |
| SHA-256 | `6A:DF:7D:CE:93:51:C9:B9:D5:1A:5F:71:92:59:04:AE:59:4D:41:21:D1:18:00:BA:3F:CB:2F:30:D3:B3:ED:5C` |

The public certificate is exported to `docs/signing/upload-certificate.pem`. Release Gradle builds compare the configured certificate with `upload-certificate.sha1` and reject a different key before executing build tasks. The previous certificate (`45:95:7D:AD:C3:92:4F:DF:C0:16:31:A9:73:F7:95:35:04:36:F3:6A`) cannot be uploaded to this Play listing.

GitHub's `ANDROID_KEYSTORE_BASE64` secret must contain the active JKS, with alias `video-downloader-premium`. `KEYSTORE_PASSWORD` and `KEY_PASSWORD` must match the local signing credentials.

Google Play signs distributed APKs with its app signing key, which may differ from this upload key. The upload key is the certificate used to sign the AAB you submit. See the [official Play App Signing guide](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en).
