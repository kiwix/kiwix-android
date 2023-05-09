import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.CustomApps
import custom.createPublisher
import custom.transactionWithCommit
import plugin.KiwixConfigurationPlugin
import java.io.FileNotFoundException
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
  productFlavors.apply {
    CustomApps.createDynamically(project.file("src"), this)
    all {
      File("$projectDir/src", "$name/$name.zim").let {
        createDownloadTask(it)
        createPublishBundleWithExpansionTask(it, applicationVariants)
      }
    }
  }
  splits {
    abi {
      isUniversalApk = true
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

fun ProductFlavor.createPublishBundleWithExpansionTask(
  file: File,
  applicationVariants: DomainObjectSet<ApplicationVariant>
): Task {
  val capitalizedName = name.capitalize()
  return tasks.create("publish${capitalizedName}ReleaseBundleWithExpansionFile") {
    group = "publishing"
    description = "Uploads $capitalizedName to the Play Console with an Expansion file"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      createPublisher(File(rootDir, "playstore.json"))
        .transactionWithCommit(packageName) {
          val variants =
            applicationVariants.releaseVariantsFor(this@createPublishBundleWithExpansionTask)
          val generatedBundleFile =
            File(
              "custom/${capitalizedName.toLowerCase()}" +
                "release/custom-${capitalizedName.toLowerCase()}-release.aab"
            )
          if (generatedBundleFile.exists()) {
            uploadBundle(generatedBundleFile)
            uploadExpansionTo(file, variants[0].versionCode)
            attachExpansionTo(variants[0].versionCode)
            addToTrackInDraft(variants[0].versionCode, versionName)
          } else {
            throw FileNotFoundException("Unable to find generated aab file")
          }
        }
    }
  }
}

fun DomainObjectSet<ApplicationVariant>.releaseVariantsFor(productFlavor: ProductFlavor) =
  find { it.name.equals("${productFlavor.name}Release", true) }!!
    .outputs.filterIsInstance<ApkVariantOutput>()
    .filter { it.baseName.contains("universal") }.sortedBy { it.versionCode }

afterEvaluate {
  tasks.filter { it.name.contains("ReleaseBundleWithExpansionFile") }.forEach {
    val flavorName =
      it.name.substringAfter("publish").substringBefore("ReleaseBundleWithExpansionFile")
    it.dependsOn.add(tasks.getByName("download${flavorName}Zim"))
    it.dependsOn.add(tasks.getByName("bundle${flavorName}Release"))
  }
}
