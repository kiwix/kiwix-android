import plugin.KiwixConfigurationPlugin

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
plugins {
  `android-library`
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}
plugins.apply(KiwixConfigurationPlugin::class)
apply(plugin = "io.objectbox")

android {
  defaultConfig {
    buildConfigField("long", "VERSION_CODE", "".getVersionCode().toString())
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.15"
  }
}

fun shouldUseLocalVersion() = File(projectDir, "libs").exists()

dependencies {
  // use jdk8 java.time backport, as long app < Build.VERSION_CODES.O
  implementation(Libs.threetenabp)

  // Get kiwixlib online if it is not populated locally
  if (!shouldUseLocalVersion()) {
    api(Libs.libkiwix)
  } else {
    implementation("com.getkeepsafe.relinker:relinker:1.4.5")
    api(fileTree(mapOf("include" to "*.aar", "dir" to "libs")))
  }

  // Document File
  implementation(Libs.select_folder_document_file)

  // Square
  implementation(Libs.converter_simplexml) {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "stax", module = "stax-api")
    exclude(group = "stax", module = "stax")
  }

  // Leak canary
  debugImplementation(Libs.leakcanary_android)

  implementation(Libs.android_arch_lifecycle_extensions)
  implementation(Libs.objectbox_kotlin)
  implementation(Libs.objectbox_rxjava)
  implementation(Libs.webkit)
  testImplementation(Libs.kotlinx_coroutines_test)
  implementation(Libs.kotlinx_coroutines_android)
  implementation(Libs.kotlinx_coroutines_rx3)
  implementation(Libs.zxing)

// Compose ans Material3 Dependencies
  implementation("androidx.compose.material3:material3-android:1.3.1")
  implementation("androidx.activity:activity-compose:1.9.3")

  implementation("androidx.compose.ui:ui:1.7.6")
  implementation("androidx.compose.material:material:1.7.6")

  implementation(platform("androidx.compose:compose-bom:2024.12.01"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material:material")
  implementation("androidx.compose.runtime:runtime-livedata")
  implementation("androidx.compose.runtime:runtime-rxjava2")

  // For testing
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
  debugImplementation("androidx.compose.ui:ui-tooling")
}
