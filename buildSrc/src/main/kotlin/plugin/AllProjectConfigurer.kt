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
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.io.File

class AllProjectConfigurer {

  fun applyPlugins(target: Project) {
    target.plugins.apply("org.jetbrains.kotlin.plugin.compose")
    // We should migrate to the ksp plugin. This is still in our project due to dagger_android_processor
    // We should think to migrate to hilt/koin.
    target.plugins.apply("com.android.legacy-kapt")
    target.plugins.apply("kotlin-parcelize")
    target.plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    target.plugins.apply("jacoco")
    target.plugins.apply("org.jlleitschuh.gradle.ktlint")
    target.plugins.apply("io.gitlab.arturbosch.detekt")
  }

  fun configureApplicationExtension(target: Project) {
    target.extensions.configure<ApplicationExtension> {
      defaultConfig {
        targetSdk = Config.targetSdk
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
      compileSdk = Config.compileSdk
      defaultConfig.apply {
        minSdk = Config.minSdk
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
      }
      target.extensions.configure<KotlinAndroidExtension> {
        compilerOptions {
          freeCompilerArgs.add("-Xjvm-default=all-compatibility")
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
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(target.files("${target.rootDir}/config/detekt/detekt.yml"))
        baseline = project.file("detekt_baseline.xml")
      }
    }
    registerDetektVariantTasks(target)
  }

  /**
   * Registers the per-variant detekt tasks (`detektDebug`, `detektCustomexampleDebug`, ...).
   *
   * detekt registers these itself, but only from inside `plugins.withId("kotlin-android")`.
   * AGP 9 makes built-in Kotlin mandatory (`android.builtInKotlin`) and the standalone
   * `kotlin-android` plugin can no longer be applied — it still casts the Android extension to
   * the removed `BaseExtension`. So detekt's Android block never runs and only the plain
   * `detekt` task survives. Until detekt ships AGP 9 support we register the variant tasks
   * ourselves, keeping detekt's task names and source sets (main + build type + flavour, no
   * test sources) so `./gradlew detektDebug detektCustomExampleDebug` keeps working in CI and
   * in the pre-commit hook.
   */
  private fun registerDetektVariantTasks(target: Project) {
    val detektExtension = target.extensions.getByType(DetektExtension::class.java)
    target.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
      ?.onVariants { registerDetektVariantTask(target, it, detektExtension) }
    target.extensions.findByType(LibraryAndroidComponentsExtension::class.java)
      ?.onVariants { registerDetektVariantTask(target, it, detektExtension) }
  }

  private fun registerDetektVariantTask(
    target: Project,
    variant: Variant,
    detektExtension: DetektExtension
  ) {
    target.tasks.register(
      "detekt${variant.name.replaceFirstChar(Char::titlecase)}",
      Detekt::class.java
    ) {
      description = "Runs detekt on the ${variant.name} variant."
      group = LifecycleBasePlugin.VERIFICATION_GROUP
      setSource(
        target.files(
          listOfNotNull(variant.sources.kotlin?.all, variant.sources.java?.all)
        )
      )
      include("**/*.kt", "**/*.kts")
      config.setFrom(detektExtension.config)
      detektExtension.baseline?.takeIf(File::exists)?.let(baseline::set)
      buildUponDefaultConfig = detektExtension.buildUponDefaultConfig
      allRules = detektExtension.allRules
      parallel = detektExtension.parallel
      ignoreFailures = detektExtension.isIgnoreFailures
      detektClasspath.setFrom(target.configurations.getByName("detekt"))
      pluginClasspath.setFrom(target.configurations.getByName("detektPlugins"))
    }
  }

  fun applyScripts(target: Project) {
    target.apply(from = "${target.rootDir}/team-props/git-hooks.gradle")
  }

  fun configureDependencies(target: Project) {
    target.dependencies {
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
      testImplementation(Libs.mockk)
      testImplementation(Libs.assertj_core)
      testImplementation(Libs.testing_ktx)
      testImplementation(Libs.core_testing)
      compileOnly(Libs.javax_annotation_api)
      implementation(Libs.dagger)
      implementation(Libs.dagger_android)
      kapt(Libs.dagger_compiler)
      // This processor does not support by ksp.
      kapt(Libs.dagger_android_processor)
      implementation(Libs.core_ktx)
      implementation(Libs.collection_ktx)
      implementation(Libs.preference_ktx)
      implementation(Libs.roomKtx)
      annotationProcessor(Libs.roomCompiler)
      implementation(Libs.roomRuntime)
      kapt(Libs.roomCompiler)
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
      testImplementation(Libs.junit_vintage_engine)
    }
  }
}
