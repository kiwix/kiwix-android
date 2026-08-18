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
