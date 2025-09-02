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
    resValue("string", "app_name", "Kiwix")
    resValue("string", "app_search_string", "Search Kiwix")
    versionCode = "".getVersionCode()
    versionName = generateVersionName()
    manifestPlaceholders["permission"] = "android.permission.MANAGE_EXTERNAL_STORAGE"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
  // By default, Android generates dependency metadata (a file containing information
  // about all the dependencies used in the project) and includes it in both APKs and app bundles.
  // This metadata is particularly useful for the Google Play Store, as it provides actionable
  // feedback on potential issues with project dependencies. However, other platforms cannot
  // utilize this metadata. For example, platforms like IzzyOnDroid, GitHub, and our website do not
  // require or utilize the metadata.
  // Since we only upload app bundles to the Play Store for kiwix app, dependency metadata
  // is enabled for app bundles to leverage Google Play Store's analysis
  // and feedback. For APKs distributed outside the Play Store, we exclude this metadata
  // as they do not require this.
  // See https://github.com/kiwix/kiwix-android/issues/4053#issuecomment-2456951610 for details.
  dependenciesInfo {
    // Disables dependency metadata when building APKs.
    // This is for the signed APKs posted on IzzyOnDroid, GitHub, and our website,
    // where dependency metadata is not required or utilized.
    includeInApk = false
    // Enables dependency metadata when building Android App Bundles.
    // This is specifically for the Google Play Store, where dependency metadata
    // is analyzed to provide actionable feedback on potential issues with dependencies.
    includeInBundle = true
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
  testImplementation(Libs.TURBINE_FLOW_TEST)
  // inject migration module in test cases.
  androidTestImplementation(project(":objectboxmigration"))
  // inject default module for all variant.
  debugImplementation(project(":defaultmigration"))
  releaseImplementation(project(":defaultmigration"))
  add("nightlyImplementation", project(":defaultmigration"))
  add("standaloneImplementation", project(":defaultmigration"))
  // inject migration module in playStore variant.
  add("playStoreImplementation", project(":objectboxmigration"))
}
tasks.register("generateVersionCodeAndName") {
  val file = File("VERSION_INFO")
  if (!file.exists()) file.createNewFile()
  file.printWriter().use {
    it.print("${generateVersionName()}\n7${"".getVersionCode()}")
  }
}

tasks.register("renameTarakFile") {
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

fun filterStringsByTags(
  file: File,
  tags: Set<String>
): List<String> {
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

gradle.projectsEvaluated {
  tasks.forEach { task ->
    if (task.name != "renameTarakFile") {
      task.dependsOn("renameTarakFile")
    }
  }
}
