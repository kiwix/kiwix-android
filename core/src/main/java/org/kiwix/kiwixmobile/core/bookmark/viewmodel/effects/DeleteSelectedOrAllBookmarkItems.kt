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

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UpdateBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State
import org.kiwix.kiwixmobile.core.data.Repository

data class DeleteSelectedOrAllBookmarkItems(
  private val state: MutableLiveData<State>,
  private val repository: Repository,
  private val actions: PublishProcessor<Action>
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    val bookmarkItems = state.value?.bookmarkItems
    if (bookmarkItems?.any { it.isSelected } == true) {
      repository.deleteBookmarks(bookmarkItems.filter { it.isSelected })
    } else if (bookmarkItems != null) {
      repository.deleteBookmarks(bookmarkItems)
      Toast.makeText(activity, R.string.all_bookmarks_cleared, Toast.LENGTH_SHORT).show()
    }
    actions.offer(UpdateBookmarks)
  }
}
