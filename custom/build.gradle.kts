import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.CustomApps
import custom.createPublisher
import custom.transactionWithCommit
import plugin.KiwixConfigurationPlugin
import java.net.URI
import java.net.URL
import java.util.Locale

plugins {
  android
}

plugins.apply(KiwixConfigurationPlugin::class)

android {
  defaultConfig {
    applicationId = "org.kiwix"
  }

  flavorDimensions += "default"
  productFlavors.apply {
    CustomApps.createDynamically(project.file("src"), this)
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
  return tasks.create(
    "download${name.replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else "$it"
    }
    }Zim"
  ) {
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
  val capitalizedName =
    name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else "$it" }
  return tasks.create("publish${capitalizedName}ReleaseApkWithExpansionFile") {
    group = "publishing"
    description = "Uploads $capitalizedName to the Play Console with an Expansion file"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      createPublisher(File(rootDir, "playstore.json"))
        .transactionWithCommit(packageName) {
          val variants =
            applicationVariants.releaseVariantsFor(this@createPublishApkWithExpansionTask)
          variants.forEach { _ -> uploadApk() }
          uploadExpansionTo(file, variants[0].versionCode.get())
          variants.drop(1).forEach { attachExpansionTo(variants[0].versionCode.get(), it) }
          addToTrackInDraft(variants)
        }
    }
  }
}

@Suppress("DEPRECATION")
fun DomainObjectSet<ApplicationVariant>.releaseVariantsFor(productFlavor: ProductFlavor) =
  find { it.name.equals("${productFlavor.name}Release", true) }!!
    .outputs.filterIsInstance<com.android.build.api.variant.VariantOutput>()
    .sortedBy { it.versionCode.get() }

afterEvaluate {
  tasks.filter { it.name.contains("ReleaseApkWithExpansionFile") }.forEach {
    val flavorName =
      it.name.substringAfter("publish").substringBefore("ReleaseApkWithExpansionFile")
    it.dependsOn.add(tasks.getByName("download${flavorName}Zim"))
    it.dependsOn.add(tasks.getByName("assemble${flavorName}Release"))
  }
}
