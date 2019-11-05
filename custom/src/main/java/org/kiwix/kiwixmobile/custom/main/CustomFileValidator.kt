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

package org.kiwix.kiwixmobile.custom.main

import android.content.Context
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasBothFiles
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasFile
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasNothing
import java.io.File
import javax.inject.Inject

class CustomFileValidator @Inject constructor(private val context: Context) {

  fun validate(onFilesFound: (ValidationState) -> Unit, onNoFilesFound: () -> Unit) =
    when (val installationState = detectInstallationState()) {
      is HasBothFiles,
      is HasFile -> onFilesFound(installationState)
      HasNothing -> onNoFilesFound()
    }

  private fun detectInstallationState(
    obbFiles: List<File> = obbFiles(),
    zimFiles: List<File> = zimFiles()
  ): ValidationState {
    return when {
      obbFiles.isNotEmpty() && zimFiles().isNotEmpty() -> HasBothFiles(obbFiles[0], zimFiles[0])
      obbFiles.isNotEmpty() -> HasFile(obbFiles[0])
      zimFiles.isNotEmpty() -> HasFile(zimFiles[0])
      else -> HasNothing
    }
  }

  private fun obbFiles() = scanDirs(ContextCompat.getObbDirs(context), "obb")

  private fun zimFiles() =
    scanDirs(ContextCompat.getExternalFilesDirs(context, null), "zim")

  private fun scanDirs(dirs: Array<out File>, extensionToMatch: String): List<File> =
    dirs.fold(listOf()) { acc, dir ->
      acc + dir.walk().filter { it.extension.startsWith(extensionToMatch) }.toList()
    }
}

sealed class ValidationState {
  data class HasBothFiles(val obbFile: File, val zimFile: File) : ValidationState()
  data class HasFile(val file: File) : ValidationState()
  object HasNothing : ValidationState()
}
