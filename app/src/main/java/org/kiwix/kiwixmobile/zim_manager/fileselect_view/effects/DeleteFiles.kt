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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimSource
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
      activity.toast(
        if (booksOnDiskListItems.deleteAll()) {
          R.string.delete_zims_toast
        } else {
          R.string.delete_zim_failed
        }
      )
    })
  }

  private fun List<BookOnDisk>.deleteAll(): Boolean {
    return fold(true) { acc, book ->
      acc && deleteSpecificZimFile(book).also {
        if (it && book.zimSource == zimReaderContainer.zimSource) {
          zimReaderContainer.setZimSource(null)
        }
      }
    }
  }

  private fun deleteSpecificZimFile(book: BookOnDisk): Boolean {
    val file = when (val source = book.zimSource) {
      is ZimSource.ZimFile -> source.file
      else -> null
    }
    file?.let { FileUtils.deleteZimFile(it.path) }
    if (file == null || file.exists()) {
      return false
    }
    newBookDao.delete(book.databaseId)
    return true
  }
}
