# TL;DR Proguard/R8 shouldn't mess with androidTest files
-dontskipnonpubliclibraryclassmembers
-dontoptimize
-dontobfuscate
-dontshrink
-ignorewarnings
-dontnote **
-dontwarn org.easymock.IArgumentMatcher
-dontwarn org.mockito.internal.creation.bytebuddy.MockMethodDispatcher
-dontwarn org.jmock.core.Constraint
-dontwarn org.robolectric.RobolectricTestRunner
-dontwarn org.objectweb.asm.ClassVisitor
-dontwarn javax.servlet.ServletContextListener
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn org.objectweb.asm.MethodVisitor
-dontwarn android.net.http.**
-dontwarn com.android.internal.http.multipart.MultipartEntity
-dontwarn java.lang.ClassValue

-keep class org.yaml.** { *; }
-keep class okreplay.** { *; }
-keepattributes InnerClasses
-keep class **.R
-keep class **.R$* {
    <fields>;
}
