<img src="https://github.com/kiwix/kiwix-android/blob/master/Kiwix_icon_transparent_512x512.png" align="left" height='250'>

<a href='https://play.google.com/store/apps/details?id=org.kiwix.kiwixmobile' target='_blank' align="right"><img align="right" height='36' src='https://s20.postimg.org/muzx3w4jh/google_play_badge.png' /></a>
# Kiwix-Android

Kiwix is an offline reader for Web content. One of its main purposes is to make Wikipedia available offline. This is done by reading the content of a file in the ZIM format, a high compressed open format with additional meta-data.

[![Build Status](https://travis-ci.org/kiwix/kiwix-android.svg?branch=master)](https://travis-ci.org/kiwix/kiwix-android)
[![IRC Web](https://img.shields.io/badge/chat-on%20freenode-brightgreen.svg)](http://chat.kiwix.org)
---

## Build Instructions

Production releases of the app should be built with our companion build repository [kiwix-build](https://github.com/kiwix/kiwix-build).

To build this repository alone for development purposes you can simply import the project into Android Studio and the hard work will be done for you. If you prefer to build without Android Studio you must first set up the Android SDK and then run the command: ```./gradlew build ``` from the root directory of the project.

We utilize different build variants (flavours) to build various different versions of our app. Ensure your build variant is **kiwixDebug** to build the standard app.

## Libraries Used

- [Dagger 2](https://github.com/google/dagger) - A fast dependency injector for Android and Java
- [SquiDb](https://github.com/yahoo/squidb) - SquiDB is a SQLite database library for Android and iOS
- [Retrofit](http://square.github.io/retrofit/) - Retrofit turns your REST API into a Java interface
- [OkHttp](https://github.com/square/okhttp) - An HTTP+SPDY client for Android and Java applications
- [Butterknife](http://jakewharton.github.io/butterknife/) - View "injection" library for Android
- [Mockito](https://github.com/mockito/mockito) - Most popular Mocking framework for unit tests written in Java 
- [Guava](https://github.com/google/guava) - Collections, caching, primitives support, concurrency libraries, common annotations, string processing, I/O, and so forth.
- [Apache](https://github.com/apache/commons-io) - The Apache Commons IO library contains utility classes, stream implementations, file filters, file comparators, endian transformation classes, and much more.
- [RxJava](https://github.com/ReactiveX/RxJava) - Reactive Extensions for the JVM – a library for composing asynchronous and event-based programs using observable sequences for the Java VM.



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

Please refer to [COPYING](COPYING).
