import org.gradle.kotlin.dsl.apply
import plugin.KiwixConfigurationPlugin

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

buildscript {
  repositories {
    google()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
  }

  dependencies {
    classpath(Libs.objectbox_gradle_plugin)
  }
}

plugins.apply(KiwixConfigurationPlugin::class)
apply(plugin = "io.objectbox")

android {
  namespace = "org.kiwix.kiwixmobile.migration"

  defaultConfig {
    minSdk = 25

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
}

dependencies {
  implementation(Libs.objectbox_kotlin)
  implementation(project(":core"))
}
