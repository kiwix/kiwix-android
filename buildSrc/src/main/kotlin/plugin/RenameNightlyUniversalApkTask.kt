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

package plugin

import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Renames the universal APK of the nightly variant, see
 * https://github.com/kiwix/kiwix-android/issues/3103
 *
 * AGP 9 removed `ApkVariantOutput.outputFileName`, which is how this used to be done. The
 * replacement is to transform the [com.android.build.api.artifact.SingleArtifact.APK] directory:
 * AGP hands us every APK the variant produced, we decide the file name for each one, and AGP
 * rewrites the accompanying `output-metadata.json` for us.
 *
 * Only the universal APK is renamed. The per-ABI splits are copied through untouched, since
 * renaming them would break the ABI suffix that the version code scheme relies on.
 */
abstract class RenameNightlyUniversalApkTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  abstract val universalApkName: Property<String>

  @get:Internal
  abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameNightlyUniversalApkTask>>

  @TaskAction
  fun renameApks() {
    transformationRequest.get().submit(this) { builtArtifact ->
      val source = File(builtArtifact.outputFile)
      val name = when (builtArtifact.outputType) {
        VariantOutputConfiguration.OutputType.UNIVERSAL -> universalApkName.get()
        else -> source.name
      }
      source.copyTo(File(outputDir.get().asFile, name), overwrite = true)
    }
  }
}
