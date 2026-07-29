# ARMSX2 
PlayStation 2 Emulator for Android based on the work of [PCSX2](https://github.com/PCSX2/pcsx2)

## App version

The Android app version is configured in `app/build.gradle.kts` under
`android.defaultConfig`:

```kotlin
versionCode = providers.gradleProperty("armsx2.versionCode").orNull?.toInt() ?: 1088
versionName = providers.gradleProperty("armsx2.versionName").orNull ?: "2.6.1"
```

Change `versionName` to the user-facing stable release version and increment the
integer `versionCode` for manually published APKs or AABs. Release scripts can
override these defaults with `-Parmsx2.versionName=...` and
`-Parmsx2.versionCode=...`.

The `Build All Platforms` GitHub Actions workflow always overrides both values
for its sideloadable GitHub APK. Its timestamp-derived `versionCode` is
monotonic and becomes the fourth numeric component of `versionName`, matching
TeknoParrotUI's updater comparison contract. Local developer builds retain the
readable defaults above.

## TeknoParrotUI companion release

The GitHub-flavor APK is a private `com.armsx2` companion module. It has no
launcher, file-open entry point, or recent-app entry; TeknoParrotUI launches it
through the signature-protected companion activity.

Default-branch CI updates the rolling `pcsx2x6` GitHub prerelease with a raw
`pcsx2x6-<version>-android-arm64.apk` asset. Configure these repository secrets
with the same production key used to sign the official TeknoParrotUI APK:

- `ANDROID_SIGNING_KEYSTORE_BASE64`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

The guarded release build fails instead of publishing a debug-signed APK when
any production-signing input is unavailable. Pull-request and non-default
branch builds remain downloadable test artifacts and never update the rolling
TPUI module release.

Before upload, CI verifies both Gradle's output metadata and the finished APK:
package `com.armsx2`, the injected four-part `versionName`, matching
`versionCode`, ARM64-only native code, valid signing, and the TPUI-only
manifest. After publishing, it also verifies the GitHub release envelope's
exact asset name, HTTPS URL, byte size, SHA-256 digest, and checksum sidecar.
The APK remains a separate updater asset and is never copied into the
TeknoParrotUI or Winlator packages.

## Android APK builds

Release/test APKs should be built with the universal page-size builder:

```bash
./tools/build-universal-page-apk.sh ~/Downloads/ARMSX2-Refresh-UniversalPage.apk
```

The script compiles both 4K and 16K ARM64 emucore variants, packages them into
one APK, signs it, and verifies 16K zip alignment. This keeps one distributable
APK working correctly on both older 4K-page devices and newer 16K-page devices.
