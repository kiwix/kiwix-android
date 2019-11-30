import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.createPublisher
import custom.transactionWithCommit
import plugin.KiwixConfigurationPlugin
import java.net.URI
import java.net.URL

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

    // Uncomment for static productFlavors
    // create(CustomApps.all)
    apply(from = "dynamic_flavors.gradle")
    all {
      File("$projectDir/src", "$name/$name.zim").let {
        createDownloadTask(it)
        createPublishApkWithExpansionTask(it, applicationVariants)
      }
    }
  }
  splits {
    abi {
      isUniversalApk = false
    }
  }
}

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

fun ProductFlavor.fetchUrl(): String {
  return URI.create(buildConfigFields["ZIM_URL"]!!.value.replace("\"", "")).toURL()
    .openConnection()
    .apply {
      connect()
      getInputStream()
    }.let {
      it.getHeaderField("Location")?.replace("https", "http") ?: it.url.toString()
    }
}

fun ProductFlavor.createPublishApkWithExpansionTask(
  file: File,
  applicationVariants: DomainObjectSet<ApplicationVariant>
): Task {
  val capitalizedName = name.capitalize()
  return tasks.create("publish${capitalizedName}ReleaseApkWithExpansionFile") {
    group = "publishing"
    description = "Uploads $capitalizedName to the Play Console with an Expansion file"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      createPublisher(File(rootDir, "google.json"))
        .transactionWithCommit(packageName) {
          val variants =
            applicationVariants.releaseVariantsFor(this@createPublishApkWithExpansionTask)
          variants.forEach(::uploadApk)
          uploadExpansionTo(file, variants[0])
          variants.drop(1).forEach { attachExpansionTo(variants[0].versionCodeOverride, it) }
          addToTrackInDraft(variants)
        }
    }
  }
}

fun DomainObjectSet<ApplicationVariant>.releaseVariantsFor(productFlavor: ProductFlavor) =
  find { it.name.equals("${productFlavor.name}Release", true) }!!
    .outputs.filterIsInstance<ApkVariantOutput>().sortedBy { it.versionCodeOverride }

afterEvaluate {
  tasks.filter { it.name.contains("ReleaseApkWithExpansionFile") }.forEach {
    val flavorName =
      it.name.substringAfter("publish").substringBefore("ReleaseApkWithExpansionFile")
    it.dependsOn.add(tasks.getByName("download${flavorName}Zim"))
    it.dependsOn.add(tasks.getByName("assemble${flavorName}Release"))
  }
}
