<a href="https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile" target="_blank" align="left">
  <img src="https://play.google.com/intl/en/badges/images/badge_new.png" alt="Get it on Google Play" height="30" />
</a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/org.kiwix.kiwixmobile.standalone" target="_blank" align="left">
  <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid2.png" alt="Get it on IzzyOnDroid" height="29" />
</a>

# Kiwix Android

Kiwix is an offline reader for Web content, primarily designed to make [Wikipedia](https://www.wikipedia.org/) available offline. It reads archives in the [ZIM](https://openzim.org) file format, a highly compressed open format with additional metadata.
This is the Android version of Kiwix, with [support versions ranging from 7.1 to 15](https://github.com/kiwix/kiwix-android/blob/main/buildSrc/src/main/kotlin/Config.kt). The app is written in [Kotlin](https://kotlinlang.org/).

[![Build Status](https://github.com/kiwix/kiwix-android/workflows/CI/badge.svg?query=branch%3Amain+workflow%3ANightly)](https://github.com/kiwix/kiwix-android/actions?query=workflow%3ACI+branch%3Amain)
[![Nightly](https://github.com/kiwix/kiwix-android/actions/workflows/nightly.yml/badge.svg)](https://github.com/kiwix/kiwix-android/actions/workflows/nightly.yml)
[![codecov](https://codecov.io/gh/kiwix/kiwix-android/branch/main/graph/badge.svg)](https://codecov.io/gh/kiwix/kiwix-android)
[![CodeFactor](https://www.codefactor.io/repository/github/kiwix/kiwix-android/badge)](https://www.codefactor.io/repository/github/kiwix/kiwix-android)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Public Chat](https://img.shields.io/badge/public-chat-green)](https://chat.kiwix.org)
[![Slack](https://img.shields.io/badge/Slack-chat-E01E5A)](https://kiwixoffline.slack.com)

## Important Notes

Starting from Android 11, Google introduced a restrictive policy that affects the Kiwix [Play Store
variant](https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile). Users can no longer directly load ZIM files from internal or external storage. The storage scanning feature has also been removed in the Play Store variant.

Actually, Kiwix Play Store variant can only directly scan/read ZIM files in reserved app directories:

| Directory name   | Storage path                                                | Readable by File manager |
|------------------|-------------------------------------------------------------|--------------------------|
| Internal public  | `storage/0/Android/media/org.kiwix.kiwixmobile/`            | Yes                      |
| Internal private | `storage/0/Android/data/org.kiwix.kiwixmobile/`             | No                       |
| External public  | `storage/sdcard-name/Android/media/org.kiwix.kiwixmobile/`  | Yes                      |
| External private | `storage/sdcard-name/Android/data/org.kiwix.kiwixmobile/`   | No                       |

### Workaround

For ZIM files downloaded through third-party apps, users can use the file picker in Kiwix to select and move these files to one of the Kiwix app's public directories (see above). This operation can be also done manually.

Be careful: Before uninstalling Kiwix, move ZIM files outside the app directory to avoid deletion.

To use the full version of Kiwix and avoid facing this restriction, you can download it directly from the [official repository](https://download.kiwix.org/release/kiwix-android/) or use [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/org.kiwix.kiwixmobile.standalone).

We understand that this restriction may cause inconvenience, but it is necessary to comply with the Play Store policies and ensure a smooth user experience. We recommend using the official version of the app available on our website to access the complete set of features.

## App variants

Starting from version 3.12.0, Kiwix is available in two variants:

- [Google Play version](https://android.kiwix.org/) using application id `org.kiwix.kiwixmobile`
- [Full version](https://download.kiwix.org/release/kiwix-android/) using application id `org.kiwix.kiwixmobile.standalone`

### Differences

**Google Play version**: Limited to scanning and opening ZIM files from reserved app directories.
**Full version**: Can load ZIM files from any storage location using the file picker.

### Why two variants?

To avoid confusion between the Google Play version and the full version, we introduced dedicated application ids (one for each variant). Using the same application id for both versions caused conflicts with the Google Play Store, as it treated them as the same app. This resulted in scenarios where Google Play would prompt updates for the full version. If users updated through the Play Store, they would lose advanced file management capabilities (such as scanning storage or directly opening ZIM files using the file picker).

This separation ensures clarity for users and prevents undesirable behavior.

### How to move content between two variants?

**Bookmarks**: Export and reimport them via the "Settings" menu.
**ZIM files**: Move them to or from the reserved app directories `Android/media/org.kiwix.kiwixmobile/` or
`Android/data/org.kiwix.kiwixmobile/`. This might have to be done for both internal storage and SD card. Kiwix can detect automatically all ZIM files available in your storage. You just need to swipe down in the local "Library" screen and
it will scan your storage and recognize all your ZIM files.

## Android permissions needed

Kiwix requires the following permissions to fully work:

- `ACCESS_FINE_LOCATION`: For device discovery on Android 12 and below.
- `NEARBY_WIFI_DEVICES`: For device discovery on Android 13 and above.
- `READ_EXTERNAL_STORAGE`: To access ZIM files.
- `WRITE_EXTERNAL_STORAGE`: To download ZIM files and export bookmarks.
- `POST_NOTIFICATIONS`: For notifications.
- `MANAGE_EXTERNAL_STORAGE`: For storage scanning on Android 11 and above (full version only).

## Build instructions
1. Clone [this repository](https://github.com/kiwix/kiwix-android).
2. Import the project with [Android Studio](https://developer.android.com/studio).
3. Set the Gradle JDK to Java 17.
4. Run ./gradlew build from the root directory.

Kiwix Android is a multi-module project, in 99% of scenarios you will want to build the `app` module in the `debug` configuration. If you are interested in our custom apps, they have their own repo [kiwix-android-custom](https://github.com/kiwix/kiwix-android-custom).

## Release

We have an [automatic version code generation](https://github.com/kiwix/kiwix-android/blob/main/buildSrc/src/main/kotlin/VersionCodeGenerator.kt) system based on the current date. However, you can override this by setting the environment variable `KIWIX_ANDROID_RELEASE_DATE` to a specific date in the `YYYY-MM-DD` format. This will use the provided date for the version code calculation instead of the current date.

### ABI Splitting for APKs

By default, `ABI` splitting is disabled. In newer Gradle versions, when uploading a `.aab` file, `ABI` splitting must remain disabled.
However, if you need to generate separate APKs for different ABIs, you can enable `ABI` splitting by setting the `APK_BUILD="true"` environment variable.
This variable should only be set when building an APK. If you set this variable and attempt to generate a `.aab` file, the build will fail due to Gradle's new enhancements.

## Libraries Used

- 📚 [Libkiwix](https://github.com/kiwix/java-libkiwix) - Kotlin/Java binding for the core Kiwix library.
- 🗡️ [Dagger 2](https://github.com/google/dagger) - Dependency injector for Android and Java.
- 🔄 [Retrofit](https://square.github.io/retrofit/) - Turns REST API into a Java interface.
- 🌐 [OkHttp](https://github.com/square/okhttp) - HTTP client for Android and Java.
- 🎭  [Mockito](https://github.com/mockito/mockito) - Mocking framework for unit tests.
- 🗃️ [ObjectBox](https://github.com/objectbox/objectbox-java) - Reactive NoSQL Database.
- 🐒 [MockK](https://github.com/mockk/mockk) - Kotlin mocking library.
- 🧪 [JUnit5](https://github.com/junit-team/junit5/) - Next-generation JUnit.
- 📥 [Fetch](https://github.com/tonyofrancis/Fetch) - File download manager library for Android.
- 🧪 [AssertJ](https://github.com/joel-costigliola/assertj-core) - Fluent assertions for test code.
- 📷 [ZXing](https://github.com/zxing/zxing) - Barcode scanning library for Java, Android.

## Contributing

Before contributing check out the [CONTRIBUTION](https://github.com/kiwix/kiwix-android/blob/main/CONTRIBUTING.md). We have automated Unit & Integration tests that run locally and on pull requests.

## Communication

* [Email](mailto:contact+android@kiwix.org)
* [Slack](https://kiwixoffline.slack.com): #android
  channel [Get an invite](https://join.slack.com/t/kiwixoffline/shared_invite/zt-19s7tsi68-xlgHdmDr5c6MJ7uFmJuBkg)

For more information, please refer to
[https://wiki.kiwix.org/wiki/Communication](https://wiki.kiwix.org/wiki/Communication).

## Support
If you're enjoying using Kiwix, drop a ⭐️ on the repo!

## License

[GPLv3](https://www.gnu.org/licenses/gpl-3.0) or later, see [COPYING](COPYING) for more details.
