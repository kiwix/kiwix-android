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

import org.gradle.api.JavaVersion

object Config {

  // Here is a list of all Android versions with their corresponding API
  // levels: https://apilevels.com/
  const val compileSdk = 33 // SDK version used by Gradle to compile our app.
  const val minSdk = 25 // Minimum SDK (Minimum Support Device) is 25 (Android 7.1 Nougat).
  const val targetSdk = 33 // Target SDK (Maximum Support Device) is 33 (Android 13).

  val javaVersion = JavaVersion.VERSION_1_8

  // Version Information
  const val versionMajor = 3 // Major version component of the app's version name and version code.
  const val versionMinor = 11 // Minor version component of the app's version name and version code.
  const val versionPatch = 0 // Patch version component of the app's version name and version code.
}
