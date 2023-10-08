import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.CustomApps
import custom.createPublisher
import custom.transactionWithCommit
import plugin.KiwixConfigurationPlugin
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.util.Locale
import java.util.Base64

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
    "download${
    name.replaceFirstChar {
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
  val urlString = buildConfigFields["ZIM_URL"]!!.value.replace("\"", "")
  var secretKey = ""
  val url = if (urlString.isAuthenticationUrl) {
    secretKey = urlString.secretKey
    URI.create(urlString.removeAuthenticationFromUrl).toURL()
  } else {
    URI.create(urlString).toURL()
  }
  return url
    .openConnection()
    .apply {
      if (urlString.isAuthenticationUrl) {
        setRequestProperty(
          "Authorization",
          "Basic ${Base64.getEncoder().encodeToString(System.getenv(secretKey).toByteArray())}"
        )
      }
      connect()
      getInputStream()
    }.let {
      it.getHeaderField("Location")?.replace("https", "http") ?: it.url.toString()
    }
}

val String.decodeUrl: String
  get() = URLDecoder.decode(this, "UTF-8")
val String.isAuthenticationUrl: Boolean
  get() = decodeUrl.trim().matches(Regex("https://[^@]+@.*\\.zim"))

val String.secretKey: String
  get() = decodeUrl.substringAfter("{{", "")
    .substringBefore("}}", "")
    .trim()

val String.removeAuthenticationFromUrl: String
  get() = decodeUrl.trim()
    .replace(Regex("\\{\\{\\s*[^}]+\\s*\\}\\}@"), "")

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
          variants.forEach(::uploadApk)
          uploadExpansionTo(file, variants[0].versionCodeOverride)
          variants.drop(1).forEach { attachExpansionTo(variants[0].versionCodeOverride, it) }
          addToTrackInDraft(variants)
        }
    }
  }
}

@Suppress("DEPRECATION")
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
