/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package plugin

import Libs
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.FilterConfiguration
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppConfigurer {
  fun configure(target: Project) {
    target.configureExtension<ApplicationExtension> {
      signingConfigs {
        create("releaseSigningConfig") {
          storeFile = File(target.rootDir, "kiwix-android.keystore")
          storePassword = System.getenv("KEY_STORE_PASSWORD") ?: "000000"
          keyAlias = System.getenv("KEY_ALIAS") ?: "keystore"
          keyPassword = System.getenv("KEY_PASSWORD") ?: "000000"
        }
      }
      buildTypes {
        getByName("release") {
          isCrunchPngs = true
          isMinifyEnabled = true
          isShrinkResources = true
          signingConfig = signingConfigs.getByName("releaseSigningConfig")
          proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            File("${target.rootDir}/app", "proguard-rules.pro")
          )
          ndk {
            // Enables including debug symbols in the Android App Bundle (AAB).
            // There’s no option to include debug symbols directly in APKs, as they
            // significantly increase the APK size. For APKs published on the Play Store,
            // the only option is to upload the debug symbols manually.
            // Two of our branded apps are published on the Play Store via APKs.
            // Apart from those, all other APKs are distributed outside the Play Store,
            // so the debug symbols are not included and do not affect their size.
            debugSymbolLevel = "FULL"
          }
        }
        getByName("debug") {
          isCrunchPngs = true
          isDebuggable = true
          if (target.hasProperty("testingMinimizedBuild")) {
            isMinifyEnabled = target.hasProperty("testingMinimizedBuild")
            isShrinkResources = false
            proguardFiles(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              File("${target.rootDir}/app", "proguard-rules.pro")
            )
            testProguardFile(File("${target.rootDir}/app", "test-rules.pro"))
          }
        }
      }

      val abiCodes = mapOf("arm64-v8a" to 6, "x86" to 3, "x86_64" to 4, "armeabi-v7a" to 5)
      splits {
        abi {
          // Enable ABI splits only when needed (e.g., when building APKs).
          // This prevents unnecessary splits when generating an App Bundle (AAB),
          // as AABs already handle ABI splits automatically.
          //
          // The environment variable `APK_BUILD` controls this behavior:
          // - If set to `"true"`, ABI splits are **enabled** (for APK builds).
          // - If `"false"` or unset, ABI splits are **disabled** (for App Bundles).
          //
          // This approach ensures that:
          // - **App Bundles (AABs)** remain unaffected (since Google Play handles ABI splits).
          // - **APK builds** get ABI splits when needed for direct distribution (e.g., custom deployments).
          //
          // See: https://github.com/kiwix/kiwix-android/issues/4273
          isEnable = System.getenv("APK_BUILD")?.toBoolean() ?: false
          reset()
          include(*abiCodes.keys.toTypedArray())
          isUniversalApk = true
        }
      }

      /*
       * Leads the version code with a one digit number corresponding
       * to the architecture (arm64-v8a, armeabi-v7a, x86_64, x86,
       * ...). If no number/architecture is found, then add "7". This is
       * should happen only for bundle (it is necessary for the Play
       * Store upgrade process that the version code is higher than
       * for APKs).
      */
      val androidComponents =
        target.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
      androidComponents.onVariants { variant ->
        variant.outputs.forEach { output ->
          val abi = output.filters
            .find { it.filterType == FilterConfiguration.FilterType.ABI }
            ?.identifier

          val abiVersionCode = abiCodes[abi] ?: 7
          output.versionCode.set(abiVersionCode * 1_000_000 + output.versionCode.get())
        }
        if (variant.name.contains("nightly", true)) {
          // this is for issue https://github.com/kiwix/kiwix-android/issues/3103
          renameNightlyUniversalApk(target, variant)
        }
      }
      sourceSets.getByName("androidTest") {
        java.directories.add("${target.rootDir}/core/src/sharedTestFunctions/java")
        kotlin.directories.add("${target.rootDir}/core/src/sharedTestFunctions/java")
      }
    }
    configureDependencies(target)
  }

  /**
   * Registers a task that transforms the variant's APK directory, renaming the universal APK.
   * See [RenameNightlyUniversalApkTask] for why this replaces the old `outputFileName` assignment.
   */
  private fun renameNightlyUniversalApk(target: Project, variant: ApplicationVariant) {
    val renameTask = target.tasks.register(
      "rename${variant.name.replaceFirstChar(Char::titlecase)}UniversalApk",
      RenameNightlyUniversalApkTask::class.java
    )
    val request = variant.artifacts
      .use(renameTask)
      .wiredWithDirectories(
        RenameNightlyUniversalApkTask::inputDir,
        RenameNightlyUniversalApkTask::outputDir
      )
      .toTransformMany(SingleArtifact.APK)
    renameTask.configure {
      transformationRequest.set(request)
      universalApkName.set(setNameForNightlyUniversalApk())
    }
  }

  private fun setNameForNightlyUniversalApk(): String =
    "org.kiwix.kiwixmobile.standalone-universal-${getCurrentDate()}.apk"

  private fun getCurrentDate() =
    Date().let(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)::format)

  private fun configureDependencies(target: Project) {
    target.dependencies {
      androidTestImplementation(Libs.espresso_core)
      androidTestImplementation(Libs.espresso_web)
      androidTestImplementation(Libs.espresso_intents)
      androidTestImplementation(Libs.espresso_contrib)
      androidTestImplementation(Libs.annotation)
      androidTestImplementation(Libs.junit)
      androidTestImplementation(Libs.junit_jupiter)
      androidTestImplementation(Libs.androidx_test_runner)
      androidTestImplementation(Libs.androidx_test_rules)
      androidTestImplementation(Libs.androidx_test_core)
      androidTestImplementation(Libs.mockwebserver)
      androidTestImplementation(Libs.barista) {
        exclude(group = "com.android.support.test.uiautomator")
      }
      androidTestImplementation(Libs.simple_xml) {
        exclude(module = "stax")
        exclude(module = "stax-api")
        exclude(module = "xpp3")
      }
      androidTestUtil(Libs.orchestrator)
      androidTestCompileOnly(Libs.javax_annotation_api)
      androidTestImplementation(Libs.hilt_android_testing)
      kspAndroidTest(Libs.hilt_android_compiler)
      androidTestImplementation(Libs.mockk_android)
      androidTestImplementation(Libs.uiautomator)
      androidTestImplementation(Libs.assertj_core)
    }
  }
}
