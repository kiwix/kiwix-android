package org.kiwix.kiwixmobile.zimManager

import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem

sealed class FileSelectActions {
  data class RequestNavigateTo(val bookOnDisk: BookOnDisk) : FileSelectActions()
  data class RequestSelect(val bookOnDisk: BookOnDisk) : FileSelectActions()
  data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : FileSelectActions()
  object RequestValidateZimFiles : FileSelectActions()
  object RequestDeleteMultiSelection : FileSelectActions()
  object RequestShareMultiSelection : FileSelectActions()
  object MultiModeFinished : FileSelectActions()
  object RestartActionMode : FileSelectActions()
  object UserClickedDownloadBooksButton : FileSelectActions()
}

data class OnlineLibraryRequest(
  val query: String? = null,
  val category: String? = null,
  val lang: String? = null,
  val isLoadMoreItem: Boolean,
  val page: Int,
  // Bug Fix #4381
  val version: Long = System.nanoTime()
)

data class OnlineLibraryResult(
  val onlineLibraryRequest: OnlineLibraryRequest,
  val books: List<LibkiwixBook>
)

data class LibraryListItemWrapper(
  val items: List<LibraryListItem>,
  val version: Long = System.nanoTime()
)
