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
import com.android.build.gradle.BaseExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AllProjectConfigurer {

  fun applyPlugins(target: Project) {
    target.plugins.apply("kotlin-android")
    target.plugins.apply("kotlin-android-extensions")
    target.plugins.apply("kotlin-kapt")
    target.plugins.apply("com.hiya.jacoco-android")
    target.plugins.apply("org.jlleitschuh.gradle.ktlint")
    target.plugins.apply("io.gitlab.arturbosch.detekt")
    target.plugins.apply("androidx.navigation.safeargs")
  }

  fun configureBaseExtension(target: Project, path: String) {
    target.configureExtension<BaseExtension> {
      setCompileSdkVersion(Config.compileSdk)
      ndkVersion = "21.4.7075529"
      defaultConfig {
        setMinSdkVersion(Config.minSdk)
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
        kotlinOptions.jvmTarget = "1.8"
      }

      testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests.apply {
          isReturnDefaultValues = true
          all(KotlinClosure1<Any, Test>({
            (this as Test).also { testTask ->
              testTask.useJUnitPlatform()
              testTask.testLogging {
                setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
                outputs.upToDateWhen { false }
                showStandardStreams = true
              }
              testTask.extensions
                .getByType(JacocoTaskExtension::class.java)
                .isIncludeNoLocationClasses = true
            }
          }, this))
        }
      }

      lintOptions {
        isAbortOnError = true
        isCheckAllWarnings = true
        isWarningsAsErrors = true

        ignore(
          "SyntheticAccessor",
          "GoogleAppIndexingApiWarning",
          "LockedOrientationActivity",
          //TODO stop ignoring below this
          "CheckResult",
          "LabelFor",
          "LogConditional",
          "ConvertToWebp"
        )

        warning(
          "UnknownNullness",
          "SelectableText",
          "MissingTranslation",
          "IconDensities",
          "ContentDescription",
          "IconDipSize"
        )
        lintConfig = target.rootProject.file("lintConfig.xml")
      }
      packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE-notice.md")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
      }
      sourceSets {
        getByName("test") {
          java.srcDir("${target.rootDir}/core/src/sharedTestFunctions/java")
          resources.srcDir("${target.rootDir}/core/src/test/resources")
        }
      }
    }
  }

  fun configurePlugins(target: Project) {
    target.run {
      configureExtension<AndroidExtensionsExtension> { isExperimental = true }
      configureExtension<JacocoPluginExtension> { toolVersion = "0.8.7" }
      configureExtension<KtlintExtension> { android.set(true) }
      configureExtension<DetektExtension> {
        baseline = project.file("detekt_baseline.xml")
      }

    }
  }

  fun applyScripts(target: Project) {
    target.apply(from = "${target.rootDir}/team-props/git-hooks.gradle")
  }

  fun configureDependencies(target: Project) {
    target.dependencies {
      implementation(Libs.kotlin_stdlib_jdk7)
      implementation(Libs.appcompat)
      implementation(Libs.material)
      implementation(Libs.constraintlayout)
      implementation(Libs.multidex)
      // navigation
      implementation(Libs.navigation_fragment_ktx)
      implementation(Libs.navigation_ui_ktx)
      androidTestImplementation(Libs.navigation_testing)
      implementation(Libs.okhttp)
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
      implementation(Libs.butterknife)
      kapt(Libs.butterknife_compiler)
      implementation(Libs.xfetch2)
      implementation(Libs.xfetch2okhttp)
      implementation(Libs.rxandroid)
      implementation(Libs.rxjava)
      implementation(Libs.preference_ktx)
    }
  }
}
