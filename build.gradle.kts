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
    maven { setUrl("https://jitpack.io") }
  }
}

tasks.register<Delete>("clean") {
  description = "Cleans the build directory of the root project."
  delete(rootProject.layout.buildDirectory)
}
