import plugin.KiwixConfigurationPlugin

buildscript {
  repositories {
    google()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
  }
}
plugins {
  `android-library`
}
plugins.apply(KiwixConfigurationPlugin::class)

fun generateVersionName() = "${Config.versionMajor}.${Config.versionMinor}.${Config.versionPatch}"

android {
  defaultConfig {
    buildConfigField("long", "VERSION_CODE", "".getVersionCode().toString())
    buildConfigField("String", "VERSION_NAME", "\"${generateVersionName()}\"")
    resValue("string", "app_name", "Kiwix")
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
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
    api(Libs.libkiwix)
  } else {
    implementation("com.getkeepsafe.relinker:relinker:1.4.5")
    api(fileTree(mapOf("include" to "*.aar", "dir" to "libs")))
  }

  // Document File
  implementation(Libs.select_folder_document_file)

  // Square
  implementation(Libs.converter_scalars) {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "stax", module = "stax-api")
    exclude(group = "stax", module = "stax")
  }
  implementation(Libs.converter_simplexml) {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "stax", module = "stax-api")
    exclude(group = "stax", module = "stax")
  }

  // Leak canary
  debugImplementation(Libs.leakcanary_android)

  implementation(Libs.android_arch_lifecycle_extensions)
  implementation(Libs.webkit)
  testImplementation(Libs.kotlinx_coroutines_test)
  implementation(Libs.kotlinx_coroutines_android)
  implementation(Libs.zxing)
  testImplementation(Libs.TURBINE_FLOW_TEST)

  // implementation("androidx.work:work-runtime-ktx:2.11.1")
}
