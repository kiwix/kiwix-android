import plugin.KiwixConfigurationPlugin

plugins {
  id("com.android.library")
}
plugins.apply(KiwixConfigurationPlugin::class)
android {
  namespace = "org.kiwix.kiwixmobile.migration"

  defaultConfig {
    minSdk = Config.minSdk

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
  api(project(":core"))
}

// This module has no unit tests of its own; it only inherits `core`'s shared test
// fixtures (MainDispatcherRule, TestApplication, etc.), none of which declare `@Test`
// methods, so the JUnit Platform has nothing to discover here.
tasks.withType<Test>().configureEach {
  failOnNoDiscoveredTests = false
}
