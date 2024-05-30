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

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.DependencyHandlerScope

internal inline fun <reified T> Project.configureExtension(function: T.() -> Unit) =
  extensions.getByType(T::class.java).function()

internal fun DependencyHandlerScope.androidTestUtil(dependency: String) =
  addDependency("androidTestUtil", dependency)

internal fun DependencyHandlerScope.kaptAndroidTest(dependency: String) =
  addDependency("kaptAndroidTest", dependency)

internal fun DependencyHandlerScope.androidTestCompileOnly(dependency: String) =
  addDependency("androidTestCompileOnly", dependency)

internal fun DependencyHandlerScope.androidTestImplementation(
  dependency: String,
  dependencyFunc: (ModuleDependency.() -> Unit)? = null
) =
  addDependency("androidTestImplementation", dependency).also {
    (it as ModuleDependency?)?.let { moduleDependency -> dependencyFunc?.invoke(moduleDependency) }
  }

internal fun DependencyHandlerScope.compileOnly(dependency: String) =
  addDependency("compileOnly", dependency)

internal fun DependencyHandlerScope.kapt(dependency: String) =
  addDependency("kapt", dependency)

internal fun DependencyHandlerScope.testImplementation(dependency: String) =
  addDependency("testImplementation", dependency)

internal fun DependencyHandlerScope.implementation(dependency: String) =
  addDependency("implementation", dependency)

internal fun DependencyHandlerScope.annotationProcessor(dependency: String) =
  addDependency("annotationProcessor", dependency)

private fun DependencyHandlerScope.addDependency(configurationName: String, dependency: String) =
  add(configurationName, dependency)
