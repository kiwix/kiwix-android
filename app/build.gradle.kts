import com.slack.keeper.optInToKeeper
import org.w3c.dom.Element
import plugin.KiwixConfigurationPlugin
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

plugins {
  android
  id("com.github.triplet.play") version Versions.com_github_triplet_play_gradle_plugin
}
if (hasProperty("testingMinimizedBuild")) {
  apply(plugin = "com.slack.keeper")
}
plugins.apply(KiwixConfigurationPlugin::class)

apply(from = rootProject.file("jacoco.gradle"))

fun generateVersionName() = "${Config.versionMajor}.${Config.versionMinor}.${Config.versionPatch}"

val apkPrefix get() = System.getenv("TAG") ?: "kiwix"

android {
  // Added namespace in response to Gradle 8.0 and above.
  // This is now specified in the Gradle configuration instead of declaring
  // it directly in the AndroidManifest file.
  namespace = "org.kiwix.kiwixmobile"
  defaultConfig {
    base.archivesName.set(apkPrefix)
    resValue("string", "app_name", "Kiwix")
    resValue("string", "app_search_string", "Search Kiwix")
    versionCode = "".getVersionCode()
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
    create("standalone") {
      initWith(getByName("release"))
      matchingFallbacks += "release"
      signingConfig = signingConfigs.getByName("releaseSigningConfig")
      applicationIdSuffix = ".standalone" // Bug Fix #3933
    }
    create("nightly") {
      initWith(getByName("debug"))
      matchingFallbacks += "debug"
      // Build the nightly APK with the released keyStore to make the APK updatable. See #3838
      signingConfig = signingConfigs.getByName("releaseSigningConfig")
      applicationIdSuffix = ".standalone" // Bug Fix #3933
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

androidComponents {
  beforeVariants { variantBuilder ->
    if (variantBuilder.name == "debug" && hasProperty("testingMinimizedBuild")) {
      variantBuilder.optInToKeeper()
    }
  }
}

dependencies {
  androidTestImplementation(Libs.leakcanary_android_instrumentation)
  testImplementation(Libs.kotlinx_coroutines_test)
}
task("generateVersionCodeAndName") {
  val file = File("VERSION_INFO")
  if (!file.exists()) file.createNewFile()
  file.printWriter().use {
    it.print(
      "${generateVersionName()}\n" +
        "7" + "".getVersionCode()
    )
  }
}

task("renameTarakFile") {
  val taraskFile = File("$rootDir/core/src/main/res/values-b+be+tarask/strings.xml")
  val mainStringsFile = File("$rootDir/core/src/main/res/values/strings.xml")

  if (taraskFile.exists() && mainStringsFile.exists()) {
    val taraskOldFile = File("core/src/main/res/values-b+be+tarask+old/strings.xml")
    if (!taraskOldFile.exists()) taraskOldFile.createNewFile()

    // Parse the main strings.xml file and extract the string tags
    val mainTags = getStringTags(mainStringsFile)

    // Parse the tarask file and filter strings based on tags present in the main strings file
    // This ensures that any string removed from the main strings file will not be
    // added to the old file, and it prevents lint errors.
    val filteredContent = filterStringsByTags(taraskFile, mainTags)

    // Write the filtered content to the taraskOldFile
    taraskOldFile.printWriter().use { writer ->
      writer.println("""<?xml version="1.0" encoding="utf-8"?>""")
      writer.println("<resources>")
      filteredContent.forEach { string ->
        writer.println("  $string")
      }
      writer.println("</resources>")
    }

    taraskFile.delete()
  }
}

fun getStringTags(file: File): Set<String> {
  val tags = mutableSetOf<String>()
  val factory = DocumentBuilderFactory.newInstance()
  val builder = factory.newDocumentBuilder()
  val doc = builder.parse(file)
  val nodeList = doc.getElementsByTagName("string")

  (0 until nodeList.length)
    .asSequence()
    .map { nodeList.item(it) as Element }
    .mapTo(tags) { it.getAttribute("name") }

  return tags
}

fun filterStringsByTags(file: File, tags: Set<String>): List<String> {
  val filteredStrings = mutableListOf<String>()
  val factory = DocumentBuilderFactory.newInstance()
  val builder = factory.newDocumentBuilder()
  val doc = builder.parse(file)
  val nodeList = doc.getElementsByTagName("string")

  for (i in 0 until nodeList.length) {
    val element = nodeList.item(i) as Element
    val name = element.getAttribute("name")
    if (name in tags) {
      filteredStrings.add(elementToString(element))
    }
  }

  return filteredStrings
}

fun elementToString(element: Element): String {
  val transformer = TransformerFactory.newInstance().newTransformer().apply {
    setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
  }
  val result = StreamResult(StringWriter())
  val source = DOMSource(element)
  transformer.transform(source, result)
  return result.writer.toString()
}

tasks.build {
  dependsOn("renameTarakFile")
}
