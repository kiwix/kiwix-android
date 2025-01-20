/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel

import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem

abstract class PageState<T : Page> {
  abstract val pageItems: List<T>
  val isInSelectionState: Boolean by lazy { pageItems.any(Page::isSelected) }
  protected val filteredPageItems: List<T> by lazy {
    pageItems.filter { showAll || it.zimId == currentZimId }
      .filter { it.title.contains(searchTerm, true) }
  }

  abstract val visiblePageItems: List<PageRelated>
  abstract val showAll: Boolean
  abstract val currentZimId: String?
  abstract val searchTerm: String

  fun getItemsAfterToggleSelectionOfItem(page: Page): List<T> {
    return pageItems.map {
      // check if the current item is `LibkiwixBookmarkItem` because we have not saving
      // the bookmarks in database so it does not have any unique value so to get the
      // selected items we check for url since url is unique for every bookmark.
      val currentItemIdentifier = if (it is LibkiwixBookmarkItem) it.url else it.id
      val pageIdentifier = if (it is LibkiwixBookmarkItem) page.url else page.id
      if (currentItemIdentifier == pageIdentifier) it.apply {
        isSelected = !isSelected
      } else it
    }
  }

  fun numberOfSelectedItems(): Int = pageItems.filter(Page::isSelected).size

  abstract fun copyWithNewItems(newItems: List<T>): PageState<T>
}
