import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.CustomApps
import custom.createPublisher
import custom.transactionWithCommit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import plugin.KiwixConfigurationPlugin
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import java.util.Locale

plugins {
  android
}

plugins.apply(KiwixConfigurationPlugin::class)

android {
  defaultConfig {
    applicationId = "org.kiwix"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  flavorDimensions += "default"
  productFlavors.apply {
    CustomApps.createDynamically(project.file("src"), this)
    all {
      // Added namespace for every custom app to make it compatible with gradle 8.0 and above.
      // This is now specified in the Gradle configuration instead of declaring
      // it directly in the AndroidManifest file.
      namespace = "org.kiwix.kiwixmobile.custom"
      File("$projectDir/src", "$name/$name.zim").let {
        createDownloadTask(it)
        createPublishApkWithExpansionTask(it, applicationVariants)
      }
      File("$projectDir/../install_time_asset/src/main/assets", "$name.zim").let {
        createDownloadTaskForPlayAssetDelivery(it)
        createPublishBundleWithAssetPlayDelivery()
      }
    }
  }

  bundle {
    language {
      // This is for testing the bundle file for the play store
      // Context: #3503
      enableSplit = false
    }
  }
  assetPacks += ":install_time_asset"
  androidResources {
    // to not compress zim file in asset folder
    noCompress.add("zim")
  }
}

dependencies {
  testImplementation(Libs.kotlinx_coroutines_test)
  testImplementation(Libs.TURBINE_FLOW_TEST)
}

fun ProductFlavor.createDownloadTask(file: File): TaskProvider<Task> {
  return tasks.register(
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

        OkHttpClient().newCall(fetchRequest()).execute().use { response ->
          if (response.isSuccessful) {
            response.body?.let { responseBody ->
              writeZimFileData(responseBody, file)
            }
          } else {
            throw RuntimeException(
              "Download Failed. Error: ${response.message}\n" +
                " Status Code: ${response.code}"
            )
          }
        }
      }
    }
  }
}

fun ProductFlavor.fetchRequest(): Request {
  val urlString = buildConfigFields["ZIM_URL"]!!.value.replace("\"", "")
  return if (urlString.isAuthenticationUrl) {
    Request.Builder()
      .url(URI.create(urlString.removeAuthenticationFromUrl).toURL())
      .header(
        "Authorization",
        "Basic " +
          Base64.getEncoder().encodeToString(System.getenv(urlString.secretKey).toByteArray())
      )
      .build()
  } else {
    Request.Builder()
      .url(URI.create(urlString).toURL())
      .build()
  }
}

fun writeZimFileData(responseBody: ResponseBody, file: File) {
  FileOutputStream(file).use { outputStream ->
    responseBody.byteStream().use { inputStream ->
      val buffer = ByteArray(4096)
      var bytesRead: Int
      while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
      }
      outputStream.flush()
    }
  }
}

fun writeZimFileDataInChunk(
  responseBody: ResponseBody,
  file: File,
  // create a chunk of 500MB
  chunkSize: Long = 500 * 1024 * 1024
) {
  var outputStream: FileOutputStream? = null
  val buffer = ByteArray(4096)
  var bytesRead: Int
  var totalBytesWritten = 0L
  var chunkNumber = 0

  responseBody.byteStream().use { inputStream ->
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
      if (outputStream == null || totalBytesWritten >= chunkSize) {
        // Close the current chunk and open a new one
        outputStream?.flush()
        outputStream?.close()
        chunkNumber++
        val nextChunkFile = File(file.parent, "chunk$chunkNumber.zim")
        nextChunkFile.createNewFile()
        outputStream = FileOutputStream(nextChunkFile)
        totalBytesWritten = 0 // Reset totalBytesWritten for the new chunk
      }

      // Write data to the output stream
      outputStream?.write(buffer, 0, bytesRead)
      totalBytesWritten += bytesRead
    }
  }

  // Close the last chunk (if any)
  outputStream?.flush()
  outputStream?.close()
}

fun ProductFlavor.createDownloadTaskForPlayAssetDelivery(file: File): TaskProvider<Task> {
  return tasks.register(
    "download${
      name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else "$it"
      }
    }ZimAndPutInAssetFolder"
  ) {
    group = "Downloading"
    doLast {
      if (file.exists()) file.delete()
      file.createNewFile()

      OkHttpClient().newCall(fetchRequest()).execute().use { response ->
        if (response.isSuccessful) {
          response.body?.let { responseBody ->
            writeZimFileDataInChunk(responseBody, file)
          }
        } else {
          throw RuntimeException(
            "Download Failed. Error: ${response.message}\n" +
              " Status Code: ${response.code}"
          )
        }
      }
    }
  }
}

val String.decodeUrl: String
  get() = URLDecoder.decode(this, "UTF-8")
val String.isAuthenticationUrl: Boolean
  get() = decodeUrl.trim().matches(Regex("https://[^@]+@.*\\.zim"))

val String.secretKey: String
  get() =
    decodeUrl.substringAfter("{{", "")
      .substringBefore("}}", "")
      .trim()

val String.removeAuthenticationFromUrl: String
  get() =
    decodeUrl
      .trim()
      .replace(Regex("\\{\\{\\s*[^}]+\\s*\\}\\}@"), "")

fun ProductFlavor.createPublishApkWithExpansionTask(
  file: File,
  applicationVariants: DomainObjectSet<ApplicationVariant>
): TaskProvider<Task> {
  val capitalizedName =
    name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else "$it" }
  return tasks.register("publish${capitalizedName}ReleaseApkWithExpansionFile") {
    group = "publishing"
    description = "Uploads $capitalizedName to the Play Console with an Expansion file"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      createPublisher(File(rootDir, "playstore.json"))
        .transactionWithCommit(packageName) {
          val variants =
            applicationVariants.releaseVariantsFor(this@createPublishApkWithExpansionTask).also {
              print("createPublishApkWithExpansionTask: $it")
            }
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
    .outputs.filterIsInstance<ApkVariantOutput>()
    .filter { !it.outputFileName.contains("universal") }
    .sortedBy { it.versionCodeOverride }

fun ProductFlavor.createPublishBundleWithAssetPlayDelivery(): TaskProvider<Task> {
  val capitalizedName =
    name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else "$it" }
  return tasks.register("publish${capitalizedName}ReleaseBundleWithPlayAssetDelivery") {
    group = "publishing"
    description = "Uploads $capitalizedName to the Play Console with an Play Asset delivery mode"
    doLast {
      val packageName = "org.kiwix$applicationIdSuffix"
      println("packageName $packageName")
      createPublisher(File(rootDir, "playstore.json"))
        .transactionWithCommit(packageName) {
          val generatedBundleFile =
            File(
              "${layout.buildDirectory.get()}/outputs/bundle/${capitalizedName.lowercase(Locale.getDefault())}" +
                "Release/custom-${capitalizedName.lowercase(Locale.getDefault())}-release.aab"
            )
          if (generatedBundleFile.exists()) {
            uploadBundle(generatedBundleFile)
            addBundleToTrackInDraft("7$versionCode".toInt(), versionName)
          } else {
            throw FileNotFoundException("Unable to find generated aab file")
          }
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
  tasks.filter { it.name.contains("ReleaseBundleWithPlayAssetDelivery") }
    .forEach { releaseBundleWithPlayAssetDeliveryTask ->
      val flavorName =
        releaseBundleWithPlayAssetDeliveryTask.name.substringAfter("publish")
          .substringBefore("ReleaseBundleWithPlayAssetDelivery")
      val downloadAndPutAssetTask = tasks.getByName("download${flavorName}ZimAndPutInAssetFolder")
      val bundleReleaseTask = tasks.getByName("bundle${flavorName}Release")
      releaseBundleWithPlayAssetDeliveryTask.dependsOn(bundleReleaseTask)
      bundleReleaseTask.dependsOn(downloadAndPutAssetTask)
    }
}
