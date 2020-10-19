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
  const val compileSdk = 28
  const val minSdk = 21
  const val targetSdk = 28
  val javaVersion = JavaVersion.VERSION_1_8

  private const val versionMajor = 3
  private const val versionMinor = 4
  private const val versionPatch = 1
  const val generatedVersionName = "$versionMajor.$versionMinor.$versionPatch"

  /*
  * max version code: 21-0-0-00-00-00
  * our template    : UU-D-A-ZZ-YY-XX
  * where:
  * X = patch version
  * Y = minor version
  * Z = major version (+ 20 to distinguish from previous, non semantic, versions of the app)
  * A = number representing ABI split
  * D = number representing density split
  * U = unused
  */
  const val generatedVersionCode =
    20 * 10000 + (versionMajor * 10000) + (versionMinor * 100) + versionPatch
}
