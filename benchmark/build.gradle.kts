plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "org.kiwix.kiwixmobile.benchmark"
  compileSdk = Config.compileSdk

  defaultConfig {
    minSdk = Config.minSdk
    targetSdk = Config.targetSdk

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = Config.javaVersion
    targetCompatibility = Config.javaVersion
  }

  kotlinOptions {
    jvmTarget = Config.javaVersion.toString()
  }

  buildTypes {
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
      proguardFile("benchmark-rules.pro")
    }
  }

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {

  implementation(Libs.junit)

  implementation(Libs.espresso_core)

  implementation(Libs.uiautomator)
  implementation(Libs.BENCHMARK_MACRO_JUNIT4)
}

androidComponents {
  beforeVariants(selector().all()) {
    it.enable = it.buildType == "benchmark"
  }
}
