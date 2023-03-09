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

import android.view.View
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.ItemBookmarkHistoryBinding
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat

sealed class NavigationHistoryViewHolder<in T : NavigationHistoryListItem>(containerView: View) :
  BaseViewHolder<T>(containerView) {

  class HistoryViewHolder(
    private val itemBookmarkHistoryBinding: ItemBookmarkHistoryBinding,
    private val onClickListener: ((NavigationHistoryListItem) -> Unit)
  ) : NavigationHistoryViewHolder<NavigationHistoryListItem>(itemBookmarkHistoryBinding.root) {
    override fun bind(item: NavigationHistoryListItem) {
      containerView.setOnClickListener { onClickListener(item) }
      itemBookmarkHistoryBinding.apply {
        title.text = item.title
        favicon.setImageDrawableCompat(R.mipmap.ic_launcher_round)
      }
    }
  }
}
