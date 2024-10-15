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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteZims
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

data class DeleteFiles(private val booksOnDiskListItems: List<BookOnDisk>) :
  SideEffect<Unit> {

  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var newBookDao: NewBookDao
  @Inject lateinit var zimReaderContainer: ZimReaderContainer

  override fun invokeWith(activity: AppCompatActivity) {
    (activity as BaseActivity).cachedComponent.inject(this)

    val name = booksOnDiskListItems.joinToString(separator = "\n") { it.book.title }

    dialogShower.show(DeleteZims(name), {
      activity.lifecycleScope.launch {
        val deleteResult = withContext(Dispatchers.IO) {
          booksOnDiskListItems.deleteAll()
        }
        activity.toast(
          if (deleteResult) {
            R.string.delete_zims_toast
          } else {
            R.string.delete_zim_failed
          }
        )
      }
    })
  }

  private suspend fun List<BookOnDisk>.deleteAll(): Boolean {
    return fold(true) { acc, book ->
      acc && deleteSpecificZimFile(book).also {
        if (it && book.zimReaderSource == zimReaderContainer.zimReaderSource) {
          zimReaderContainer.setZimReaderSource(null)
        }
      }
    }
  }

  private suspend fun deleteSpecificZimFile(book: BookOnDisk): Boolean {
    val file = book.zimReaderSource.file
    file?.let {
      @Suppress("UnreachableCode")
      FileUtils.deleteZimFile(it.path)
    }
    if (file?.isFileExist() == true) {
      return false
    }
    newBookDao.delete(book.databaseId)
    return true
  }
}
