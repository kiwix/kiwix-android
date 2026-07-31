/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package branded

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.Variant
import org.gradle.api.provider.Provider
import org.gradle.api.file.Directory
import java.io.File
import kotlin.text.get

class ApkCollector {
  data class ApkInfo(
    val apkDirectory: Provider<Directory>,
    val loader: BuiltArtifactsLoader
  )

  private val releaseApks = mutableMapOf<String, ApkInfo>()

  fun register(variant: Variant) {
    if (variant.buildType != "release") return

    val flavor = variant.productFlavors.single().second

    releaseApks[flavor] = ApkInfo(
      apkDirectory = variant.artifacts.get(SingleArtifact.APK),
      loader = variant.artifacts.getBuiltArtifactsLoader()
    )
  }

  fun findReleaseApks(flavor: String): List<ReleaseApk> {
    val info = releaseApks[flavor] ?: error("No release APK registered for flavor '$flavor'")

    val artifacts = info.loader.load(info.apkDirectory.get())
      ?: error("No APK artifacts found for flavor '$flavor'")

    return artifacts.elements
      .filterNot { artifact ->
        artifact.filters.any { it.identifier == "universal" }
      }
      .sortedBy { it.versionCode }
      .map {
        ReleaseApk(
          apkFile = File(it.outputFile),
          versionCode = it.versionCode,
          versionName = it.versionName
        )
      }
  }
}
