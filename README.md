Kiwix is an offline reader for Web content. One of its main purposes is to make Wikipedia available offline. This is done by reading the content of a file in the ZIM format, a high compressed open format with additional meta-data.

Production releases of the app should be built with our companion build repository [kiwix-build](https://github.com/kiwix/kiwix-build).

To build this repository alone for development purposes you can simply import the project into Android Studio and the hard work will be done for you. If you prefer to build without Android Studio you must first set up the Android SDK and then run the command:
```./gradlew build ``` from the root directory of the project.

<b>We utilize different build variants (flavours) to build various different versions of our app. Ensure your build variant is kiwixDebug to build the standard app.</b>

Before contributing be sure to check out https://github.com/kiwix/kiwix-android/blob/master/CONTRIBUTING.md.

We currently have a series of automated Unit and Integration tests. These can be run locally and are also run when submitting a pull request.



*********************************************************************
*************************** CONTACT *********************************
*********************************************************************

Email: kiwix-developer@lists.sourceforge.net or contact@kiwix.org
Jabber: kelson@kiwix.org
IRC: #kiwix on irc.freenode.net

You can use IRC web interface on http://chat.kiwix.org/

More... http://wiki.kiwix.org/wiki/Communication

*********************************************************************
********************** LEGAL & DISCLAIMER ***************************
*********************************************************************

Read '../COPYING' file

### Libraries:
- [Guava](https://github.com/google/guava)
- [Dagger](https://github.com/google/dagger)
- [SquiDB](https://github.com/yahoo/squidb)
- [Apache](https://github.com/apache/commons-io)
- [RxJava](https://github.com/ReactiveX/RxAndroid)
- [ButterKnife](https://github.com/JakeWharton/butterknife)
- [Retrofit](https://github.com/square/retrofit) + [OkHttp](https://github.com/square/okhttp)
- Testing:
  - [JUnit4](https://github.com/junit-team/junit4)
  - [Mockito](https://github.com/mockito/mockito)

