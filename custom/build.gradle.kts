import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.createPublisher
import custom.transactionWithCommit
import plugin.KiwixConfigurationPlugin
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  android
}
plugins.apply(KiwixConfigurationPlugin::class)

android {
  defaultConfig {
    applicationId = "org.kiwix"
  }

  flavorDimensions("default")
  productFlavors {

    create(
      CustomFlavor(
        flavorName = "customexample",
        versionName = "2017-07",
        url = "http://download.kiwix.org/zim/wikipedia_fr_test.zim",
        enforcedLanguage = "en",
        appName = "Test Custom App"
      ),
      CustomFlavor(
        flavorName = "wikimed",
        versionName = "2018-08",
        url = "http://download.kiwix.org/zim/wikipedia_en_medicine_novid.zim",
        enforcedLanguage = "en",
        appName = "Medical Wikipedia"
      )
    )
    all {
      val zimFile = File("$projectDir/src", "$name/$name.zim")
      createDownloadTask(zimFile)
      createPublishApkWithExpansionTask(name, zimFile, applicationVariants)
    }
  }
  splits {
    abi {
      isUniversalApk = false
    }
  }
}

// apply(from = File("dynamic_flavors.gradle"))

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}

fun ProductFlavor.fetchUrl(): String {
  val urlConnection =
    URI.create(buildConfigFields["ZIM_URL"]!!.value.replace("\"", "")).toURL()
      .openConnection()
  urlConnection.connect()
  urlConnection.getInputStream()
  return urlConnection
    .getHeaderField("Location")
    ?.replace("https", "http")
    ?: urlConnection.url.toString()
}

fun NamedDomainObjectContainer<ProductFlavor>.create(vararg customFlavors: CustomFlavor) {
  customFlavors.forEach { customFlavor ->
    create(customFlavor.flavorName) {
      versionName = customFlavor.versionName
      versionCode = customFlavor.versionCode
      applicationIdSuffix = ".kiwixcustom${customFlavor.flavorName}"
      buildConfigField("String", "ZIM_URL", "\"${customFlavor.url}\"")
      buildConfigField("String", "ENFORCED_LANG", "\"${customFlavor.enforcedLanguage}\"")
      configureStrings(customFlavor.appName)
    }
  }
}

data class CustomFlavor(
  val flavorName: String,
  val versionName: String,
  val versionCode: Int = Date().let {
    SimpleDateFormat("YYDDD0").format(it).toInt()
  },
  val url: String,
  val enforcedLanguage: String,
  val appName: String
)

fun ProductFlavor.createDownloadTask(file: File): Task {
  return tasks.create("download${name.capitalize()}Zim") {
    group = "Downloading"
    doLast {
      if (!file.exists()) {
        file.createNewFile()
        URL(fetchUrl()).openStream().use {
          it.copyTo(file.outputStream())
        }
      }
    }
  }
}

fun ProductFlavor.createPublishApkWithExpansionTask(
  flavorName: String,
  file: File,
  applicationVariants: DomainObjectSet<ApplicationVariant>
): Task {
  return tasks.create("publish${flavorName.capitalize()}ReleaseApkWithExpansionFile") {
    group = "publishing"
    description = "Uploads ${flavorName.capitalize()} to the Play Console with an Expansion file"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      val apkVariants = getApkVariants(applicationVariants, flavorName)
      createPublisher(File(rootDir, "google.json"))
        .transactionWithCommit(packageName) {
          apkVariants.forEach(::uploadApk)
          uploadExpansionTo(file, apkVariants[0])
          apkVariants.drop(1).forEach {
            attachExpansionTo(apkVariants[0].versionCodeOverride, it)
          }
          addToTrackInDraft(apkVariants)
        }
    }
  }
}
afterEvaluate {
  tasks.filter { it.name.contains("ReleaseApkWithExpansionFile") }.forEach {
    val flavorName =
      it.name.substringAfter("publish").substringBefore("ReleaseApkWithExpansionFile")
    it.dependsOn.add(tasks.getByName("download${flavorName}Zim"))
    it.dependsOn.add(tasks.getByName("assemble${flavorName}Release"))
  }
}

fun getApkVariants(applicationVariants: DomainObjectSet<ApplicationVariant>, flavorName: String) =
  applicationVariants.find {
    it.name.contains("release", true) && it.name.contains(flavorName, true)
  }!!.outputs.filterIsInstance<ApkVariantOutput>().sortedBy { it.versionCodeOverride }
