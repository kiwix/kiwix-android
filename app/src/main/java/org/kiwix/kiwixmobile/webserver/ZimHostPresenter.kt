/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.webserver

import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.webserver.ZimHostContract.Presenter
import org.kiwix.kiwixmobile.webserver.ZimHostContract.View
import javax.inject.Inject

@ActivityScope
class ZimHostPresenter @Inject internal constructor(
  private val dataSource: DataSource
) : BasePresenter<View>(), Presenter {
  override suspend fun loadBooks(previouslyHostedBookIds: Set<String>) {
    runCatching {
      val books = dataSource.getLanguageCategorizedBooks().first()
      books.forEach { item ->
        if (item is BooksOnDiskListItem.BookOnDisk) {
          item.isSelected = when {
            // Hosted books are now saved using the unique book ID.
            previouslyHostedBookIds.contains(item.book.id) -> true
            // Backward compatibility: for users who have not been migrated to the new logic yet,
            // fall back to checking only the title.
            previouslyHostedBookIds.contains(item.book.title) -> true
            // If no previously hosted books are saved, select all books by default.
            previouslyHostedBookIds.isEmpty() -> true
            else -> false
          }
        }
      }
      view?.addBooks(books)
    }.onFailure {
      Log.e(TAG, "Unable to load books", it)
    }
  }

  companion object {
    private const val TAG = "ZimHostPresenter"
  }
}
