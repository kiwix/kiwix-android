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
import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AppConfigurer {
  fun configure(target: Project) {
    target.configureExtension<AppExtension> {
      defaultConfig {
        vectorDrawables.useSupportLibrary = true
      }
      dexOptions {
        javaMaxHeapSize = "4g"
      }
      splits {
        abi {
          isEnable = true
          reset()
          include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
          isUniversalApk = true
        }
      }
      testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
      }
      aaptOptions {
        cruncherEnabled = true
      }
    }

    configureDependencies(target)
  }

  private fun configureDependencies(target: Project) {
    target.dependencies {
      this.add("implementation", project(":core"))
      androidTestImplementation(Libs.espresso_core)
      androidTestImplementation(Libs.espresso_web)
      androidTestImplementation(Libs.espresso_intents)
      androidTestImplementation(Libs.espresso_contrib)
      androidTestImplementation(Libs.androidx_annotation)
      androidTestImplementation(Libs.junit)
      androidTestImplementation(Libs.junit_jupiter)
      androidTestImplementation(Libs.androidx_test_runner)
      androidTestImplementation(Libs.androidx_test_rules)
      androidTestImplementation(Libs.androidx_test_core)
      androidTestImplementation(Libs.mockwebserver)
      androidTestUtil(Libs.orchestrator)
      androidTestImplementation(Libs.mockito_android)
      androidTestCompileOnly(Libs.javax_annotation_api)
      kaptAndroidTest(Libs.dagger_compiler)
      androidTestImplementation(Libs.mockk_android)
      androidTestImplementation(Libs.uiautomator)
      androidTestImplementation(Libs.assertj_core)
    }
  }
}
