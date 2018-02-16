# Kiwix-Android

[![Build Status](https://travis-ci.org/kiwix/kiwix-android.svg?branch=master)](https://travis-ci.org/kiwix/kiwix-android)

Kiwix is an offline reader for Web content. One of its main purposes is to make Wikipedia available offline. This is done by reading the content of a file in the ZIM format, a high compressed open format with additional meta-data.

## Build Instructions

Production releases of the app should be built with our companion build repository [kiwix-build](https://github.com/kiwix/kiwix-build).

To build this repository alone for development purposes you can simply import the project into Android Studio and the hard work will be done for you. If you prefer to build without Android Studio you must first set up the Android SDK and then run the command: ```./gradlew build ``` from the root directory of the project.

We utilize different build variants (flavours) to build various different versions of our app. Ensure your build variant is **kiwixDebug** to build the standard app.

## Contributing

Before contributing be sure to check out the [CONTRIBUTION](https://github.com/kiwix/kiwix-android/blob/master/CONTRIBUTING.md) guidelines.

We currently have a series of automated Unit and Integration tests. These can be run locally and are also run when submitting a pull request.

## Code Style
For contributions please read the [CODESTYLE](docs/codestyle.md) carefully. Pull requests that do not match the style will be rejected.

## Commit Style
For writing commit messages please read the [COMMITSTYLE](docs/commitstyle.md) carefully. Kindly adhere to the guidelines. Pull requests not matching the style will be rejected.  

## Communication

Please use IRC to discuss questions regarding the project: #kiwix on irc.freenode.net

You can use IRC web interface on [http://chat.kiwix.org/](http://chat.kiwix.org/).

Our other sources of communications include

- Email: kiwix-developer@lists.sourceforge.net or contact@kiwix.org
- Jabber: kelson@kiwix.org

For more information, please refer to [http://wiki.kiwix.org/wiki/Communication](http://wiki.kiwix.org/wiki/Communication).


## LEGAL & DISCLAIMER

Read '../COPYING' file
