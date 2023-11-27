# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

########################
# Kiwix specific rules #
########################
-dontobfuscate

-keepclassmembers class org.kiwix.videowebview.VideoEnabledWebView$JavascriptInterface { public *; }

#keep everything in libkiwix
-keep class org.kiwix.libkiwix.** { *; }

#keep everything in libzim
-keep class org.kiwix.libzim.** { *; }

## SimpleXml

-keep public class org.simpleframework.** { *; }
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.core.** { *; }
-keep class org.simpleframework.xml.util.** { *; }

-keepattributes ElementList, Root

-keepclassmembers class * {
    @org.simpleframework.xml.* *;
}

## keep everything in MetaLinkNetworkEntity.kt

-keepnames class org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity$*
-keepclassmembers class org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity$* {
    <init>(...);
}

-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLEventReader
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.events.Attribute
-dontwarn javax.xml.stream.events.Characters
-dontwarn javax.xml.stream.events.StartElement
-dontwarn javax.xml.stream.events.XMLEvent

-keep class okhttp3.internal.** { *; }
-dontwarn okhttp3.internal.Version
-dontwarn okhttp3.internal.annotations.EverythingIsNonNull
-dontwarn okhttp3.internal.http.HttpDate
-dontwarn okhttp3.internal.http.UnrepeatableRequestBody

-keep class org.bouncycastle.jsse.** { *; }
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider

-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier

-keep class org.openjsse.javax.net.ssl.** { *; }
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
