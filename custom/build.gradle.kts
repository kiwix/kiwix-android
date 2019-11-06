import com.android.build.gradle.internal.dsl.ProductFlavor
import plugin.KiwixConfigurationPlugin

plugins {
  android
}
plugins.apply(KiwixConfigurationPlugin::class)

android {
  defaultConfig {
    applicationId = "org.kiwix"
  }

  flavorDimensions("default")

  // productFlavors {
  //   create("customexample") {
  //     versionName = "2017-07"
  //     versionCode = 1
  //     applicationIdSuffix = ".kiwixcustomexample"
  //     configureStrings("Test Custom App")
  //   }
  // }
}

apply(from = File("build_custom.gradle"))

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}
