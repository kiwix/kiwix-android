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
package org.kiwix.kiwixmobile.core.help

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.ItemHelpBinding
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.collapse
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.expand

internal class HelpAdapter(titleDescriptionMap: Map<String, String>) :
  RecyclerView.Adapter<HelpAdapter.Item>() {
  private var helpItems = titleDescriptionMap.map { (key, value) -> HelpItem(key, value) }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): Item = Item(ItemHelpBinding.inflate(LayoutInflater.from(parent.context), parent, false))

  override fun onBindViewHolder(
    holder: Item,
    position: Int
  ) {
    holder.bind(helpItems[position])
  }

  override fun getItemCount(): Int = helpItems.size

  internal inner class Item(private val itemHelpBinding: ItemHelpBinding) :
    BaseViewHolder<HelpItem>(itemHelpBinding.root) {

    @SuppressWarnings("MagicNumber")
    fun toggleDescriptionVisibility() {
      if (itemHelpBinding.itemHelpDescription.visibility == View.GONE) {
        ObjectAnimator.ofFloat(itemHelpBinding.itemHelpToggleExpand, "rotation", 0f, 180f).start()
        itemHelpBinding.itemHelpDescription.expand()
      } else {
        ObjectAnimator.ofFloat(itemHelpBinding.itemHelpToggleExpand, "rotation", 180f, 360f).start()
        itemHelpBinding.itemHelpDescription.collapse()
      }
    }

    override fun bind(item: HelpItem) {
      itemHelpBinding.itemHelpTitle.setOnClickListener { toggleDescriptionVisibility() }
      itemHelpBinding.itemHelpToggleExpand.setOnClickListener { toggleDescriptionVisibility() }
      itemHelpBinding.itemHelpDescription.text = item.description
      itemHelpBinding.itemHelpTitle.text = item.title
    }
  }
}

class HelpItem(val title: String, val description: String)
