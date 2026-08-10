plugins {
  id("com.android.test")
}

android {
  namespace = "org.kiwix.kiwixmobile.benchmark"
  compileSdk = Config.compileSdk

  defaultConfig {
    minSdk = Config.minSdk
    targetSdk = Config.targetSdk

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
  }

  compileOptions.apply {
    encoding = "UTF-8"
    sourceCompatibility = Config.javaVersion
    targetCompatibility = Config.javaVersion
  }

  buildTypes {
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += "release"
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
  add("benchmarkImplementation", project(":objectboxmigration"))
}

androidComponents {
  beforeVariants(selector().all()) {
    it.enable = it.buildType == "benchmark"
  }
}
