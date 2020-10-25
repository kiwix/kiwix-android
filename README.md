<img src="https://github.com/kiwix/kiwix-android/blob/develop/Kiwix_icon_transparent_512x512.png" align="right" height='250' />
<a href="https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile" target="_blank" align="left">
  <img src="https://play.google.com/intl/en/badges/images/badge_new.png" alt="Get it on Google Play" height="30" />
</a>
<a href="https://f-droid.org/fr/packages/org.kiwix.kiwixmobile/" target="_blank" align="left">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/0/0d/Get_it_on_F-Droid.svg/320px-Get_it_on_F-Droid.svg.png" alt="Get it on F-Droid" height="29" />
</a>

# Kiwix Android

Kiwix is an offline reader for Web content. One of its main purpose
is to make Wikipedia available offline. This is done by reading the
content of a file in the ZIM format, a highly compressed open format
with additional meta-data. This is the version for Android.

Kiwix Android is written in [Kotlin](https://kotlinlang.org/) (with a few old
pieces in Java).

[![Build Status](https://github.com/kiwix/kiwix-android/workflows/CI/badge.svg?query=branch%3Adevelop+workflow%3ANightly)](https://github.com/kiwix/kiwix-android/actions?query=workflow%3ANightly+branch%3Adevelop)
[![codecov](https://codecov.io/gh/kiwix/kiwix-android/branch/develop/graph/badge.svg)](https://codecov.io/gh/kiwix/kiwix-android)
[![CodeFactor](https://www.codefactor.io/repository/github/kiwix/kiwix-android/badge)](https://www.codefactor.io/repository/github/kiwix/kiwix-android)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Public Chat](https://img.shields.io/badge/public-chat-green)](https://chat.kiwix.org)
[![Slack](https://img.shields.io/badge/Slack-chat-E01E5A)](https://kiwixoffline.slack.com)

## Build Instructions

Production releases of the app are built on travis and released automatically

To build this repository alone for development purposes you can simply
**import** the project into Android Studio and the hard work will be done
for you. Note here that instead of *opening* the project, you have to *import* it. If you prefer to build without Android Studio you must first
set up the Android SDK and then run the command: `./gradlew build `
from the root directory of the project.

Kiwix-Android is a multi-module project, in 99% of scenarios you will want to build the `app` module in the `debug` configuration.
If you are interested in our custom apps they have their own repo [kiwix-android-custom](https://github.com/kiwix/kiwix-android-custom
) that goes into further detail

## Contributing

Before contributing be sure to check out the
[CONTRIBUTION](https://github.com/kiwix/kiwix-android/blob/develop/CONTRIBUTING.md)
guidelines.

We currently have a series of automated Unit and Integration
tests. These can be run locally and are also run when submitting a
pull request.

## Code Style

For contributions please read the [CODESTYLE](docs/codestyle.md)
carefully. Pull requests that do not match the style will be rejected.

## Commit Style

For writing commit messages please read the
[COMMITSTYLE](docs/commitstyle.md) carefully. Kindly adhere to the
guidelines. Pull requests not matching the style will be rejected.

## Communication

Available communication channels:
* [Web Public Chat channel](https://chat.kiwix.org)
* [Email](mailto:contact+android@kiwix.org)
* [Mailing list](mailto:kiwix-developer@lists.sourceforge.net)
* [Slack](https://kiwixoffline.slack.com): #android channel [Get an invite](https://join.slack.com/t/kiwixoffline/shared_invite/enQtOTUyMTg4NzMxMTM4LTU0MzYyZDliYjdmMDYzYWMzNDA0MDc4MWE5OGM0ODFhYjAxNWIxMjVjZTU4MTkyODJlZWFkMmQ2YTZkYTUzZDY)
* IRC: #kiwix on irc.freenode.net

For more information, please refer to
[https://wiki.kiwix.org/wiki/Communication](https://wiki.kiwix.org/wiki/Communication).

## License

[GPLv3](https://www.gnu.org/licenses/gpl-3.0) or later, see
[COPYING](COPYING) for more details.
