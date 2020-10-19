import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import com.github.triplet.gradle.androidpublisher.ResolutionStrategy
import plugin.KiwixConfigurationPlugin

plugins {
  android
  id("com.github.triplet.play") version Versions.com_github_triplet_play_gradle_plugin
}
plugins.apply(KiwixConfigurationPlugin::class)

apply(from = rootProject.file("jacoco.gradle"))

val apkPrefix get() = System.getenv("TAG") ?: "dev"

android {

  defaultConfig {
    base.archivesBaseName = apkPrefix
    resValue("string", "app_name", "Kiwix")
    resValue("string", "app_search_string", "Search Kiwix")
    versionCode = Config.generatedVersionCode
    versionName = Config.generatedVersionName
  }

  lintOptions {
    isCheckDependencies = true
  }

  buildTypes {
    getByName("debug") {
      multiDexKeepProguard = file("multidex-instrumentation-config.pro")
      buildConfigField("boolean", "KIWIX_ERROR_ACTIVITY", "false")
    }

    getByName("release") {
      buildConfigField("boolean", "KIWIX_ERROR_ACTIVITY", "true")
      if (properties.containsKey("disableSigning")) {
        signingConfig = null
      }
    }
  }

  sourceSets {
    getByName("androidTest") {
      java.srcDirs("$rootDir/core/src/sharedTestFunctions/java")
    }
  }
}

play {
  enabled.set(true)
  serviceAccountCredentials.set(file("../google.json"))
  track.set("alpha")
  releaseStatus.set(ReleaseStatus.DRAFT)
  resolutionStrategy.set(ResolutionStrategy.FAIL)
}

dependencies {
  implementation(Libs.squidb)
  implementation(Libs.squidb_annotations)
  implementation(Libs.ink_page_indicator)
  add("kapt", Libs.squidb_processor)
}
