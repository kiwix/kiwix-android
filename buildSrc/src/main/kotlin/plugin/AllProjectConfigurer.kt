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

import Config
import Libs
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AllProjectConfigurer {

  fun applyPlugins(target: Project) {
    target.plugins.apply("org.jetbrains.kotlin.plugin.compose")
    // `:playstoremodule` applies kapt itself (see its build.gradle.kts) for the ObjectBox
    // annotation processor, which doesn't support ksp. Kotlin Gradle Plugin forces a task-graph
    // dependency between kapt's stub generation and ksp whenever both are applied to the same
    // module, which is circular here, so ksp is skipped for that module. Dagger/Hilt's compiler
    // only needs ksp everywhere else, now that the unused dagger-android/dagger-android-processor
    // (which forced kapt project-wide) has been removed - see issue #5023.
    if (target.name != "playstoremodule") {
      target.plugins.apply("com.google.devtools.ksp")
    } else {
      target.plugins.apply("com.android.legacy-kapt")
    }
    target.plugins.apply("dagger.hilt.android.plugin")
    target.plugins.apply("kotlin-parcelize")
    target.plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    target.plugins.apply("jacoco")
    target.plugins.apply("org.jlleitschuh.gradle.ktlint")
    target.plugins.apply("dev.detekt")
  }

  fun configureApplicationExtension(target: Project) {
    target.extensions.configure<ApplicationExtension> {
      defaultConfig {
        targetSdk = Config.TARGET_SDK
      }
      configureBaseExtensions(this, target)
    }
  }

  fun configureLibraryExtension(target: Project) {
    target.extensions.configure<LibraryExtension> {
      // The namespace cannot be directly set in `LibraryExtension`.
      // The core module is configured as a library for both Kiwix and branded apps.
      // Therefore, we set the namespace in `BaseExtension` for the core module,
      // based on the boolean value of `isLibrary`. This value is passed from the
      // `KiwixConfigurationPlugin`. If the current plugin is `LibraryPlugin`,
      // indicating it is the core module, then this value will be true,
      // and we set the namespace accordingly.
      namespace = "org.kiwix.kiwixmobile.core"
      configureBaseExtensions(this, target)
    }
  }

  private fun configureBaseExtensions(
    extension: CommonExtension,
    target: Project
  ) {
    with(extension) {
      // Using the same NDK version as in `java-libkiwix`, because with the default Gradle NDK,
      // the debug symbols are not included in the Android App Bundle (AAB).
      ndkVersion = Config.NDK_VERSION
      compileSdk = Config.COMPILE_SDK
      defaultConfig.apply {
        minSdk = Config.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      }

      buildTypes.named("debug") {
        enableUnitTestCoverage = true
        enableAndroidTestCoverage = true
      }

      compileOptions.apply {
        encoding = "UTF-8"
        sourceCompatibility = Config.javaVersion
        targetCompatibility = Config.javaVersion
        isCoreLibraryDesugaringEnabled = true
      }
      target.extensions.configure<KotlinAndroidExtension> {
        compilerOptions {
          jvmDefault.set(JvmDefaultMode.ENABLE)
        }
      }
      buildFeatures.apply {
        viewBinding = true
        /*
         * By default, the generation of the `BuildConfig` class is turned off in Gradle `8.1.3`.
         * Since we are setting and using `buildConfig` properties in our project,
         * enabling this attribute will generate the `BuildConfig` file.
         */
        buildConfig = true
        compose = true
        resValues = true
      }

      testOptions.apply {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.apply {
          isReturnDefaultValues = true
          isIncludeAndroidResources = true
          all {
            it.also { testTask ->
              testTask.useJUnitPlatform()
              // Robolectric fetches android-all jars directly via HTTP at test runtime,
              // bypassing Gradle's own repositories — point it at the same mirror to avoid
              // repo1.maven.org's 429s (see https://robolectric.org/configuring/).
              testTask.systemProperty(
                "robolectric.dependency.repo.url",
                "https://maven-central.storage-download.googleapis.com/maven2"
              )
              testTask.systemProperty(
                "robolectric.dependency.repo.id",
                "google-maven-central-mirror"
              )
              testTask.testLogging {
                setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
                testTask.outputs.upToDateWhen { false }
                showStandardStreams = true
              }
              testTask.extensions
                .getByType(JacocoTaskExtension::class.java).apply {
                  isIncludeNoLocationClasses = true
                  excludes = listOf("jdk.internal.*")
                }
            }
          }
        }
      }
      packaging.apply {
        resources.excludes.apply {
          add("META-INF/DEPENDENCIES")
          add("META-INF/LICENSE")
          add("META-INF/LICENSE.txt")
          add("META-INF/LICENSE.md")
          add("META-INF/LICENSE-notice.md")
          add("META-INF/license.txt")
          add("META-INF/NOTICE")
          add("META-INF/NOTICE.txt")
          add("META-INF/notice.txt")
          add("META-INF/ASL2.0")
        }
        jniLibs.useLegacyPackaging = false
      }
      sourceSets.named("test") {
        java.directories.add("${target.rootDir}/core/src/sharedTestFunctions/java")
        kotlin.directories.add("${target.rootDir}/core/src/sharedTestFunctions/java")
        resources.directories.add("${target.rootDir}/core/src/test/resources")
      }

      lint.apply {
        abortOnError = true
        checkAllWarnings = true
        warningsAsErrors = true

        disable.apply {
          add("SyntheticAccessor")
          add("GoogleAppIndexingApiWarning")
          add("LockedOrientationActivity")
          // TODO stop ignoring below this
          add("LabelFor")
          add("ConvertToWebp")
          add("UnknownNullness")
          add("SelectableText")
          add("MissingTranslation")
          add("IconDensities")
          add("IconDipSize")
          add("UnusedResources")
          add("NonConstantResourceId")
          add("NotifyDataSetChanged")
          add("Aligned16KB") // TODO Remove when properly migrated to Android 16.
          add("AndroidGradlePluginVersion")
          add("MemberExtensionConflict")
        }
        lintConfig = target.rootProject.file("lintConfig.xml")
      }
    }
  }

  fun configureJacoco(target: Project) {
    target.configurations.all {
      resolutionStrategy {
        eachDependency {
          if ("org.jacoco" == this.requested.group) {
            useVersion("0.8.14")
          }
        }
      }
    }
  }

  fun configurePlugins(target: Project) {
    target.run {
      configureExtension<JacocoPluginExtension> { toolVersion = "0.8.15" }
      configureExtension<KtlintExtension> { android.set(true) }
      configureExtension<DetektExtension> {
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        // Equivalent to the old `build: maxIssues: 0` (zero-tolerance) behavior removed in
        // detekt 2.0's config.yml schema: `Info` is the lowest severity level that still fails
        // the build (as opposed to `Never`), so this fails on any reported issue.
        failOnSeverity.set(FailOnSeverity.Info)
        config.setFrom(target.files("${target.rootDir}/config/detekt/detekt.yml"))
        baseline.set(project.file("detekt_baseline.xml"))
      }
    }
  }

  fun applyScripts(target: Project) {
    target.apply(from = "${target.rootDir}/team-props/git-hooks.gradle")
  }

  fun configureDependencies(target: Project) {
    target.dependencies {
      coreLibraryDesugaring(Libs.DESUGAR_JDK_LIBS)
      implementation(Libs.KOTLIN_STDLIB_JDK8)
      implementation(Libs.appcompat)
      implementation(Libs.appcompat_resource)
      implementation(Libs.material)
      implementation(Libs.logging_interceptor)
      implementation(Libs.retrofit)
      testImplementation(Libs.TURBINE_FLOW_TEST)
      testImplementation(Libs.kotlinx_coroutines_test)
      testImplementation(Libs.junit_jupiter)
      testRuntimeOnly(Libs.JUNIT_PLATFORM_LAUNCHER)
      testRuntimeOnly(Libs.JUNIT_VINTAGE_ENGINE)
      testImplementation(Libs.mockk)
      testImplementation(Libs.mockwebserver)
      testImplementation(Libs.assertj_core)
      testImplementation(Libs.testing_ktx)
      testImplementation(Libs.core_testing)
      compileOnly(Libs.javax_annotation_api)
      implementation(Libs.HILT_ANDROID)
      // `:playstoremodule`'s `DatabaseModule.providesBoxStore()` has an `@Provides` method
      // whose body references the ObjectBox-generated `MyObjectBox`, so the annotation
      // processor still needs to run there to emit `DatabaseModule_ProvidesBoxStoreFactory`.
      // It has to run via kapt rather than ksp in this one module though: ksp and kapt would
      // otherwise need to resolve each other's output first (Dagger needs `MyObjectBox` to
      // exist, kapt's stub generation is forced to wait on ksp by Kotlin Gradle Plugin), which
      // is circular. A single kaptDebugKotlin task can run both the ObjectBox and Hilt
      // annotation processors without that conflict.
      if (target.name != "playstoremodule") {
        ksp(Libs.HILT_ANDROID_COMPILER)
      } else {
        kapt(Libs.HILT_ANDROID_COMPILER)
      }
      implementation(Libs.core_ktx)
      implementation(Libs.collection_ktx)
      implementation(Libs.preference_ktx)
      implementation(Libs.roomKtx)
      annotationProcessor(Libs.roomCompiler)
      implementation(Libs.roomRuntime)
      if (target.name != "playstoremodule") {
        ksp(Libs.roomCompiler)
      }
      implementation(Libs.tracing)
      implementation(Libs.fetch)
      implementation(Libs.fetchOkhttp)
      implementation(Libs.androidx_activity)
      androidTestImplementation(Libs.leakcanary_android_instrumentation)

      // compose
      implementation(Libs.COMPOSE_MATERIAL3)
      implementation(Libs.ANDROIDX_ACTIVITY_COMPOSE)
      implementation(Libs.COMPOSE_TOOLING_PREVIEW)
      implementation(Libs.COMPOSE_LIVE_DATA)
      implementation(Libs.COIL3_COMPOSE)
      implementation(Libs.COIL3_OKHTTP_COMPOSE)
      implementation(Libs.COMPOSE_NAVIGATION)
      implementation(Libs.LIFECYCLE_VIEWMODEL_COMPOSE)
      implementation(Libs.LIFECYCLE_VIEWMODEL_KTX)
      implementation(Libs.ACCOMPANIST)

      // Jetpack Datastore
      implementation(Libs.DATASTORE)

      // Compose UI test implementation
      androidTestImplementation(Libs.COMPOSE_UI_TEST_JUNIT)
      androidTestImplementation(Libs.COMPOSE_UI_TEST_JUNIT_ACCESSIBILITY)
      testImplementation(Libs.COMPOSE_UI_TEST_JUNIT)
      debugImplementation(Libs.COMPOSE_UI_MANIFEST)
      debugImplementation(Libs.COMPOSE_TOOLING)

      // Robolectric unit testing
      testImplementation(Libs.robolectric)
      testImplementation(Libs.androidx_test_core)

      // Dagger Hilt
      implementation(Libs.HILT_NAVIGATION_COMPOSE)
    }
  }
}
