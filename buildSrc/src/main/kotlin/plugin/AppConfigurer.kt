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
      aaptOptions {
        cruncherEnabled = true
      }
    }

    configureDependencies(target)
  }

  private fun configureDependencies(target: Project) {
    target.dependencies {
      add("implementation", project(":core"))
    }
  }
}
