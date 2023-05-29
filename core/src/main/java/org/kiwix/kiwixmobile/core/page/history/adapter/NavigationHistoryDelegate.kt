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

package org.kiwix.kiwixmobile.core.page.history.adapter

import android.view.ViewGroup
import org.kiwix.kiwixmobile.core.base.adapter.AbsDelegateAdapter
import org.kiwix.kiwixmobile.core.databinding.ItemBookmarkHistoryBinding
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.viewBinding

sealed class NavigationHistoryDelegate<I : NavigationHistoryListItem,
  out VH : NavigationHistoryViewHolder<I>> :
  AbsDelegateAdapter<I, NavigationHistoryListItem, VH> {

  class NavigationDelegate(private val onClickListener: ((NavigationHistoryListItem) -> Unit)) :
    NavigationHistoryDelegate<NavigationHistoryListItem,
      NavigationHistoryViewHolder.HistoryViewHolder>() {
    override val itemClass = NavigationHistoryListItem::class.java

    override fun createViewHolder(parent: ViewGroup) =
      NavigationHistoryViewHolder.HistoryViewHolder(
        parent.viewBinding(ItemBookmarkHistoryBinding::inflate, false),
        onClickListener
      )
  }
}
