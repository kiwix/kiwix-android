import plugin.KiwixConfigurationPlugin

buildscript {
  repositories {
    google()
    mavenCentral()
    jcenter()
  }

  dependencies {
    classpath(Libs.objectbox_gradle_plugin)
    classpath(Libs.butterknife_gradle_plugin)
  }
}
plugins {
  `android-library`
}
plugins.apply(KiwixConfigurationPlugin::class)
apply(plugin = "io.objectbox")
apply(plugin = "com.jakewharton.butterknife")

android {
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}

fun shouldUseLocalVersion() = File(projectDir, "libs").exists()

dependencies {

  // use jdk8 java.time backport, as long app < Build.VERSION_CODES.O
  implementation(Libs.threetenabp)

  // Get kiwixlib online if it is not populated locally
  if (!shouldUseLocalVersion()) {
    api(Libs.kiwixlib)
  } else {
    implementation("com.getkeepsafe.relinker:relinker:1.3.1")
    api(fileTree(mapOf("include" to "*.aar", "dir" to "libs")))
  }

  // SquiDB
  implementation(Libs.squidb)
  implementation(Libs.squidb_annotations)
  add("kapt", Libs.squidb_processor)

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
}
