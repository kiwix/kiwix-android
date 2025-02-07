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
  id("org.jetbrains.kotlin.plugin.compose") version Versions.org_jetbrains_kotlin_plugin_compose
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
    kotlinCompilerExtensionVersion = Versions.kotlin_compiler_extension_version
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

  implementation(Libs.androidx_compose_material3)
  implementation(Libs.androidx_activity_compose)

  implementation(Libs.androidx_compose_ui)
  implementation(platform(Libs.androidx_compose_bom))
  implementation(Libs.androidx_compose_ui_tooling)
  implementation(Libs.androidx_compose_runtime_livedata)
  implementation(Libs.androidx_compose_runtime_rxjava2)

  // For Compose UI Testing
  androidTestImplementation(Libs.androidx_compose_ui_test_junit4)
  debugImplementation(Libs.androidx_compose_ui_tooling)
}
