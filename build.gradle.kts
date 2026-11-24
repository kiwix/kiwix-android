import org.gradle.kotlin.dsl.register

buildscript {
  repositories {
    google()
    maven { setUrl("https://maven-central.storage-download.googleapis.com/maven2/") }
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
  }
  dependencies {
    classpath(Libs.com_android_tools_build_gradle)
    classpath(Libs.USEFULNESS_KEEPER)
    classpath(Libs.KOTLIN_KSP)

    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}
plugins {
  buildSrcVersions
  id("org.jetbrains.kotlin.plugin.compose") version Versions.ORG_JETBRAINS_KOTLIN apply false
  id("org.jetbrains.kotlin.plugin.serialization") version Versions.ORG_JETBRAINS_KOTLIN apply false
}

allprojects {
  repositories {
    google()
    maven { setUrl("https://maven-central.storage-download.googleapis.com/maven2/") }
    mavenCentral()
    // `com.github.tonyofrancis.Fetch` only exists on JitPack. Without this, Gradle still
    // probes google()/the mirror/mavenCentral() first, and a transient 429 from any of those
    // (rather than a clean 404) aborts resolution before it ever reaches JitPack. `exclusiveContent`
    // routes this group straight to JitPack, skipping the other repos for it entirely.
    exclusiveContent {
      forRepository { maven { setUrl("https://jitpack.io") } }
      filter { includeGroup("com.github.tonyofrancis.Fetch") }
    }
  }
}

tasks.register<Delete>("clean") {
  description = "Cleans the build directory of the root project."
  delete(rootProject.layout.buildDirectory)
}
