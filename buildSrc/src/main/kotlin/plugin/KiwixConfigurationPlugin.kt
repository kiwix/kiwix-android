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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class KiwixConfigurationPlugin : Plugin<Project> {

  private val allProjectConfigurer = AllProjectConfigurer()
  private val appConfigurer = AppConfigurer()

  override fun apply(target: Project) {
    allProjectConfigurer.applyPlugins(target)
    target.plugins.all {
      when (this) {
        is LibraryPlugin -> {
          doDefaultConfiguration(target, true)
        }
        is AppPlugin -> {
          doDefaultConfiguration(target, false)
          appConfigurer.configure(target)
        }
      }
    }
    allProjectConfigurer.configurePlugins(target)
    allProjectConfigurer.applyScripts(target)
    allProjectConfigurer.configureDependencies(target)
    allProjectConfigurer.configureJacoco(target)
  }

  private fun doDefaultConfiguration(target: Project, isLibrary: Boolean) {
    allProjectConfigurer.configureBaseExtension(target, isLibrary)
    allProjectConfigurer.configureCommonExtension(target)
  }
}
