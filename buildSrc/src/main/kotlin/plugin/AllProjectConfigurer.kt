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
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AllProjectConfigurer {

  fun applyPlugins(target: Project) {
    target.plugins.apply("kotlin-android")
    target.plugins.apply("kotlin-kapt")
    target.plugins.apply("com.google.devtools.ksp")
    target.plugins.apply("kotlin-parcelize")
    target.plugins.apply("jacoco")
    target.plugins.apply("org.jlleitschuh.gradle.ktlint")
    target.plugins.apply("io.gitlab.arturbosch.detekt")
    target.plugins.apply("androidx.navigation.safeargs")
  }

  fun configureBaseExtension(target: Project, isLibrary: Boolean) {
    target.configureExtension<BaseExtension> {
      // The namespace cannot be directly set in `LibraryExtension`.
      // The core module is configured as a library for both Kiwix and custom apps.
      // Therefore, we set the namespace in `BaseExtension` for the core module,
      // based on the boolean value of `isLibrary`. This value is passed from the
      // `KiwixConfigurationPlugin`. If the current plugin is `LibraryPlugin`,
      // indicating it is the core module, then this value will be true,
      // and we set the namespace accordingly.
      if (isLibrary) {
        namespace = "org.kiwix.kiwixmobile.core"
      }
      setCompileSdkVersion(Config.compileSdk)
      defaultConfig {
        minSdk = Config.minSdk
        setTargetSdkVersion(Config.targetSdk)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      }

      buildTypes {
        getByName("debug") {
          isTestCoverageEnabled = true
          multiDexEnabled = true
        }
      }

      compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = Config.javaVersion
        targetCompatibility = Config.javaVersion
      }
      target.tasks.withType(KotlinCompile::class.java) {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
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
      }

      testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.apply {
          isReturnDefaultValues = true
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
      packagingOptions {
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
      sourceSets {
        getByName("test") {
          java.srcDir("${target.rootDir}/core/src/sharedTestFunctions/java")
          resources.srcDir("${target.rootDir}/core/src/test/resources")
        }
      }
    }
  }

  fun configureCommonExtension(target: Project) {
    target.configureExtension<CommonExtension<*, *, *, *, *, *>> {
      lint {
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
            useVersion("0.8.12")
          }
        }
      }
    }
  }

  fun configurePlugins(target: Project) {
    target.run {
      configureExtension<JacocoPluginExtension> { toolVersion = "0.8.8" }
      configureExtension<KtlintExtension> { android.set(true) }
      configureExtension<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config = target.files("${target.rootDir}/config/detekt/detekt.yml")
        baseline = project.file("detekt_baseline.xml")
      }
    }
  }

  fun applyScripts(target: Project) {
    target.apply(from = "${target.rootDir}/team-props/git-hooks.gradle")
  }

  fun configureDependencies(target: Project) {
    target.dependencies {
      implementation(Libs.kotlin_stdlib_jdk8)
      implementation(Libs.appcompat)
      implementation(Libs.appcompat_resource)
      implementation(Libs.material)
      implementation(Libs.constraintlayout)
      implementation(Libs.swipe_refresh_layout)
      implementation(Libs.multidex)
      // navigation
      implementation(Libs.navigation_fragment_ktx)
      implementation(Libs.navigation_ui_ktx)
      androidTestImplementation(Libs.navigation_testing)
      implementation(Libs.logging_interceptor)
      implementation(Libs.retrofit)
      implementation(Libs.adapter_rxjava2)
      testImplementation(Libs.junit_jupiter)
      testImplementation(Libs.mockk)
      testImplementation(Libs.assertj_core)
      testImplementation(Libs.testing_ktx)
      testImplementation(Libs.core_testing)
      compileOnly(Libs.javax_annotation_api)
      implementation(Libs.dagger)
      implementation(Libs.dagger_android)
      kapt(Libs.dagger_compiler)
      kapt(Libs.dagger_android_processor)
      implementation(Libs.core_ktx)
      implementation(Libs.fragment_ktx)
      implementation(Libs.collection_ktx)
      implementation(Libs.rxandroid)
      implementation(Libs.rxjava)
      implementation(Libs.preference_ktx)
      implementation(Libs.material_show_case_view)
      implementation(Libs.roomKtx)
      annotationProcessor(Libs.roomCompiler)
      implementation(Libs.roomRuntime)
      implementation(Libs.roomRxjava2)
      kapt(Libs.roomCompiler)
      implementation(Libs.tracing)
      implementation(Libs.fetch)
      implementation(Libs.fetchOkhttp)
      implementation(Libs.androidx_activity)
    }
  }
}
