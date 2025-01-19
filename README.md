<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Kiwix_logo_v3_glow.png/512px-Kiwix_logo_v3_glow.png" align="right" height='250' />
<a href="https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile" target="_blank" align="left">
  <img src="https://play.google.com/intl/en/badges/images/badge_new.png" alt="Get it on Google Play" height="30" />
</a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/org.kiwix.kiwixmobile" target="_blank" align="left">
  <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid2.png" alt="Get it on IzzyOnDroid" height="29" />
</a>

# Kiwix Android

Kiwix is an offline reader for Web content. One of its main purposes
is to make [Wikipedia](https://www.wikipedia.org/) available
offline. This is achieved by reading archives in the
[ZIM](https://openzim.org) file format, a highly compressed open format
with additional metadata.

This is the version for Android, with [support versions ranging from 7.1
to 15](https://github.com/kiwix/kiwix-android/blob/main/buildSrc/src/main/kotlin/Config.kt).

Kiwix Android is written in [Kotlin](https://kotlinlang.org/).

[![Build Status](https://github.com/kiwix/kiwix-android/workflows/CI/badge.svg?query=branch%3Amain+workflow%3ANightly)](https://github.com/kiwix/kiwix-android/actions?query=workflow%3ACI+branch%3Amain)
[![Nightly](https://github.com/kiwix/kiwix-android/actions/workflows/nightly.yml/badge.svg)](https://github.com/kiwix/kiwix-android/actions/workflows/nightly.yml)
[![codecov](https://codecov.io/gh/kiwix/kiwix-android/branch/main/graph/badge.svg)](https://codecov.io/gh/kiwix/kiwix-android)
[![CodeFactor](https://www.codefactor.io/repository/github/kiwix/kiwix-android/badge)](https://www.codefactor.io/repository/github/kiwix/kiwix-android)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Public Chat](https://img.shields.io/badge/public-chat-green)](https://chat.kiwix.org)
[![Slack](https://img.shields.io/badge/Slack-chat-E01E5A)](https://kiwixoffline.slack.com)

## Important Notes

Starting from Android 11, Google has introduced a new very restrictive
policy which concretly forbids Kiwix [Play Store
variant](https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile)
users to directly load ZIM files from internal/external storage. For
the same reason, and still only in the Play Store variant, the storage
scanning feature has been removed.

Actualy, Kiwix Play Store variant can only directly scan/read ZIM
files in reserved app directories:

| Directory name   | Storage path                                                | Readable by File manager |
|------------------|-------------------------------------------------------------|--------------------------|
| Internal public  | `storage/0/Android/media/org.kiwix.kiwixmobile/`            | Yes                      |
| Internal private | `storage/0/Android/data/org.kiwix.kiwixmobile/`             | No                       |
| External public  | `storage/sdcard-name/Android/media/org.kiwix.kiwixmobile/`  | Yes                      |
| External private | `storage/sdcard-name/Android/data/org.kiwix.kiwixmobile/`   | No                       |

As a workaround, for ZIM files downloaded through third party apps,
Kiwix users can use the file picker (in Kiwix library) to select these
ZIM files... which then will be copied/moved to one of the the Kiwix
app public directories (see above). An operation which can also be
done manually.

Be careful, before uninstalling Kiwix, if the user wants to keep its
ZIM files, he will have to move them outside of the app
directory. Otherwise, the ZIM file might be removed during the
process.

To use the full version of Kiwix and avoid to suffer of this
restriction, you can download it directly from the [official
repository](https://download.kiwix.org/release/kiwix-android/) or use
[IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/org.kiwix.kiwixmobile).

We understand that this restriction may cause inconvenience, but it is
necessary to comply with the Play Store policies and ensure a smooth
user experience.  We recommend using the official version of the app
available on our website to access the complete set of features.

## App variants

Starting from version `3.12.0`, Kiwix is available in two variants which are slightly different:

- [Google Play version](https://android.kiwix.org/) using application id `org.kiwix.kiwixmobile`
- [Full version](https://download.kiwix.org/release/kiwix-android/) using application id `org.kiwix.kiwixmobile.standalone`

### What are the difference between the two variants?

While the core functionalities remain the same, the primary difference
lies in the ability to access ZIM files located outside the (Kiwix)
reserved app directories.

The Google Play version has a limitation when it comes to scan and
open ZIM files from anywhere in your device storage, as mentioned in
the [Important Notes](#important-notes).

In contrast, the full version of the application can load (and scan
for) ZIM files directly from other storage locations via the file
picker... without requiring them to be copied or moved to the reserved
app directories.

### Why two variants?

To avoid confusion between the Google Play version and the full
version, we introduced dedicated application ids (one for each
variant). Using the same application id for both versions caused
conflicts with the Google Play Store, as it treated them as the same
app. This resulted in scenarios where Google Play would prompt updates
for the full version. If users updated through the Play Store, they
would lose advanced file management capabilities (such as scanning
storage or directly opening ZIM files using the file picker).

This separation ensures clarity for users and prevents undesirable
behavior.

### How to move content between two variants?

You can move the bookmarks by exporting and reimporting them. The
feature is available in the "Settings".

Regarding ZIM files, you should move them to - or from - the reserved
app directories `Android/media/org.kiwix.kiwixmobile/` or
`Android/data/org.kiwix.kiwixmobile/`. This might have to be done for
both internal storage and SD card.

Kiwix can detect automatically all ZIM files available in your
storage. You just need to swipe down in the local "Library" screen and
it will scan your storage and recognize all your ZIM files.

## Android permissions needed

Kiwix requires the following permissions to fully work:

- `ACCESS_FINE_LOCATION`: Required on devices running Android 12 and below to discover nearby 
   devices when transferring ZIM files.
- `NEARBY_WIFI_DEVICES`: Required on devices running Android 13 and above to discover nearby devices
   for transferring ZIM files.
- `READ_EXTERNAL_STORAGE`: Required to access and read ZIM files stored on the device.
- `WRITE_EXTERNAL_STORAGE`: Required to download ZIM files, export bookmarks, save notes, etc.
- `POST_NOTIFICATIONS`: Required to display notifications for ongoing downloads, active hotspots, 
   and the read-aloud feature.
- `MANAGE_EXTERNAL_STORAGE`: Required on Android 11 and above to scan the storage and locate all
   ZIM files. This permission is only available in the full version of the application.

## Build instructions

To build Kiwix Android, clone [this
repository](https://github.com/kiwix/kiwix-android) and import (not
open) the project with [Android
Studio](https://developer.android.com/studio).

If you prefer to build without Android Studio you must first set up
the Android SDK and then run the command: `./gradlew build ` from the
root directory of the project. The project requires `Java 17` to run,
Therefore set the `Gradle JDK` to `Java 17`.

Kiwix Android is a multi-module project, in 99% of scenarios you will
want to build the `app` module in the `debug` configuration. If you
are interested in our custom apps, they have their own repo
[kiwix-android-custom](https://github.com/kiwix/kiwix-android-custom).

## Release

We have an [automatic version code generation](https://github.com/kiwix/kiwix-android/blob/main/buildSrc/src/main/kotlin/VersionCodeGenerator.kt) system based on the current date. However, you 
can override this by setting the environment variable `KIWIX_ANDROID_RELEASE_DATE` to a specific
date in the `YYYY-MM-DD` format. This will use the provided date for the version code calculation
instead of the current date.

## Libraries Used

- [Libkiwix](https://github.com/kiwix/java-libkiwix) - Kotlin/Java binding for the core Kiwix
  library
- [Dagger 2](https://github.com/google/dagger) - A fast dependency injector for Android and Java
- [Retrofit](https://square.github.io/retrofit/) - Retrofit turns your REST API into a Java
  interface
- [OkHttp](https://github.com/square/okhttp) - An HTTP+SPDY client for Android and Java applications
- [Mockito](https://github.com/mockito/mockito) - Most popular Mocking framework for unit tests
  written in Java
- [RxJava](https://github.com/ReactiveX/RxJava) - Reactive Extensions for the JVM – a library for
  composing asynchronous and event-based programs using observable sequences for the Java VM.
- [ObjectBox](https://github.com/objectbox/objectbox-java) - Reactive NoSQL Database
- [MockK](https://github.com/mockk/mockk) - Kotlin mocking library that allows mocking of final
  classes by default.
- [JUnit5](https://github.com/junit-team/junit5/) - The next generation of JUnit
- [AssertJ](https://github.com/joel-costigliola/assertj-core) - Fluent assertions for test code
- [ZXing](https://github.com/zxing/zxing) - Barcode scanning library for Java, Android
- [Fetch](https://github.com/tonyofrancis/Fetch) - A customizable file download manager library for
    Android

## Contributing

Before contributing be sure to check out the
[CONTRIBUTION](https://github.com/kiwix/kiwix-android/blob/main/CONTRIBUTING.md)
guidelines.

We currently have a series of automated Unit & Integration
tests. These can be run locally and are also run when submitting a
pull request.

## Communication

Available communication channels:

* [Email](mailto:contact+android@kiwix.org)
* [Slack](https://kiwixoffline.slack.com): #android
  channel [Get an invite](https://join.slack.com/t/kiwixoffline/shared_invite/zt-19s7tsi68-xlgHdmDr5c6MJ7uFmJuBkg)

For more information, please refer to
[https://wiki.kiwix.org/wiki/Communication](https://wiki.kiwix.org/wiki/Communication).

## License

[GPLv3](https://www.gnu.org/licenses/gpl-3.0) or later, see
[COPYING](COPYING) for more details.
