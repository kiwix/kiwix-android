import plugin.KiwixConfigurationPlugin

plugins {
  android
  id("com.github.triplet.play") version Versions.com_github_triplet_play_gradle_plugin
}
plugins.apply(KiwixConfigurationPlugin::class)

apply(from = rootProject.file("jacoco.gradle"))

fun generateVersionName() = "${Config.versionMajor}.${Config.versionMinor}.${Config.versionPatch}"

val apkPrefix get() = System.getenv("TAG") ?: "kiwix"

android {

  defaultConfig {
    base.archivesName.set(apkPrefix)
    resValue("string", "app_name", "Kiwix")
    resValue("string", "app_search_string", "Search Kiwix")
    versionCode = GenerateVersionCode.getVersionCode()
    versionName = generateVersionName()
    manifestPlaceholders["permission"] = "android.permission.MANAGE_EXTERNAL_STORAGE"
  }
  lint {
    checkDependencies = true
  }

  buildTypes {
    getByName("debug") {
      multiDexKeepProguard = file("multidex-instrumentation-config.pro")
      buildConfigField("boolean", "KIWIX_ERROR_ACTIVITY", "false")
      buildConfigField("boolean", "IS_PLAYSTORE", "false")
    }

    getByName("release") {
      buildConfigField("boolean", "KIWIX_ERROR_ACTIVITY", "true")
      buildConfigField("boolean", "IS_PLAYSTORE", "false")
      if (properties.containsKey("disableSigning")) {
        signingConfig = null
      }
    }
    create("playStore") {
      manifestPlaceholders += mapOf()
      initWith(getByName("release"))
      matchingFallbacks += "release"
      buildConfigField("boolean", "IS_PLAYSTORE", "true")
      manifestPlaceholders["permission"] = "android.permission.placeholder"
    }
    create("nightly") {
      initWith(getByName("debug"))
      matchingFallbacks += "debug"
      // Build the nightly APK with the released keyStore to make the APK updatable. See #3838
      signingConfig = signingConfigs.getByName("releaseSigningConfig")
    }
  }
  bundle {
    language {
      // This is disabled so that the App Bundle does NOT split the APK for each language.
      // We're gonna use the same APK for all languages.
      enableSplit = false
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
  serviceAccountCredentials.set(file("../playstore.json"))
  track.set("internal")
  releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
  resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.FAIL)
}

dependencies {
  androidTestImplementation(Libs.leakcanary_android_instrumentation)
}
task("generateVersionCodeAndName") {
  val file = File("VERSION_INFO")
  if (!file.exists()) file.createNewFile()
  file.printWriter().use {
    it.print(
      "${generateVersionName()}\n" +
        "7${GenerateVersionCode.getVersionCode()}"
    )
  }
}

task("renameTarakFile") {
  val taraskFile = File("core/src/main/res/values-b+be+tarask/strings.xml")
  if (taraskFile.exists()) {
    val taraskOldFile = File("core/src/main/res/values-b+be+tarask+old/strings.xml")
    if (!taraskOldFile.exists()) taraskOldFile.createNewFile()
    taraskOldFile.printWriter().use {
      it.print(taraskFile.readText())
    }
    taraskFile.delete()
  }
}

tasks.build {
  dependsOn("renameTarakFile")
}
