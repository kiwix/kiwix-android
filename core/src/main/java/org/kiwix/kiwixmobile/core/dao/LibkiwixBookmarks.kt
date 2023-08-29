/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import io.objectbox.kotlin.query
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity_
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.libkiwix.Manager
import javax.inject.Inject

class LibkiwixBookmarks @Inject constructor(val manager: Manager) : PageDao {
  fun bookmarks(): Flowable<List<Page>> = box.asFlowable(
    box.query {
      order(BookmarkEntity_.bookmarkTitle)
    }
  ).map { it.map(org.kiwix.kiwixmobile.core.page.bookmark.adapter::BookmarkItem) }
  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) {
  }
}
