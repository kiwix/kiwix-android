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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

interface FileOperationHandler {
  suspend fun copy(
    sourceUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  )

  suspend fun move(
    selectedFile: DocumentFile,
    sourceUri: Uri,
    destinationFolderUri: Uri,
    destinationFile: File,
    onProgress: suspend (Int) -> Unit
  ): Boolean

  fun rollbackMove(
    destinationFile: File,
    originalParentUri: Uri
  ): Boolean

  suspend fun delete(uri: Uri, selectedFile: DocumentFile): Boolean
}
