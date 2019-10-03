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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class KiwixConfigurationPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.all {
      when (this) {
        is LibraryPlugin -> {
          target.configureExtension<LibraryExtension> { configure(target.projectDir.toString()) }
        }
        is AppPlugin -> {
          target.configureExtension<AppExtension> { configure(target.projectDir.toString()) }
        }
      }
    }
    target.plugins.apply("kotlin-android")
    target.plugins.apply("kotlin-android-extensions")
    target.plugins.apply("kotlin-kapt")
    target.plugins.apply("jacoco-android")
    target.plugins.apply("org.jlleitschuh.gradle.ktlint")
    target.configureExtension<AndroidExtensionsExtension> { isExperimental = true }
    target.configureExtension<JacocoPluginExtension> { toolVersion = "0.8.3" }
    target.configureExtension<KtlintExtension> { android.set(true) }
    target.apply(from = "${target.rootDir}/team-props/git-hooks.gradle")
    target.dependencies {
      testImplementation(Libs.junit_jupiter)
      testImplementation(Libs.mockk)
      testImplementation(Libs.assertj_core)
      testImplementation(Libs.testing_ktx)
      testImplementation(Libs.core_testing)
    }
  }
}

private fun DependencyHandlerScope.testImplementation(dependency: String) =
  add("testImplementation", dependency)

private inline fun <reified T> Project.configureExtension(function: T.() -> Unit) =
  extensions.getByType(T::class.java).function()

private fun LibraryExtension.configure(path: String) {
  baseConfigure(path)
}

private fun AppExtension.configure(path: String) {
  baseConfigure(path)
}

private fun BaseExtension.baseConfigure(path: String) {
  setCompileSdkVersion(Config.compileSdk)

  defaultConfig {
    setMinSdkVersion(Config.minSdk)
    setTargetSdkVersion(Config.targetSdk)
  }

  compileOptions.apply {
    encoding = "UTF-8"
    sourceCompatibility = Config.javaVersion
    targetCompatibility = Config.javaVersion
  }

  testOptions {
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
    isCheckAllWarnings = true

    ignore(
      "SyntheticAccessor",
      //TODO stop ignoring below this
      "MissingTranslation",
      "CheckResult",
      "LabelFor",
      "DuplicateStrings",
      "LogConditional"
    )

    warning(
      "UnknownNullness",
      "SelectableText",
      "IconDensities",
      "SyntheticAccessor"
    )
    baseline("${path}/lint-baseline.xml")
  }
}
