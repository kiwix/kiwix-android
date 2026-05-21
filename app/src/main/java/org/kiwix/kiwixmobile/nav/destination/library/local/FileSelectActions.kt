/*
 * Kiwix Android
 * Copyright (c) 2019-2026 Kiwix <android.kiwix.org>
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

import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk

/**
 * Sealed class representing all file selection actions.
 */
sealed class FileSelectActions {
  data class RequestNavigateTo(val bookOnDisk: BookOnDisk) : FileSelectActions()
  data class RequestSelect(val bookOnDisk: BookOnDisk) : FileSelectActions()
  data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : FileSelectActions()
  data object RequestValidateZimFiles : FileSelectActions()
  data object RequestDeleteMultiSelection : FileSelectActions()
  data object RequestShareMultiSelection : FileSelectActions()
  data object MultiModeFinished : FileSelectActions()
  data object RestartActionMode : FileSelectActions()
  data object UserClickedDownloadBooksButton : FileSelectActions()
}
