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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.collapse
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.expand
import kotlinx.android.synthetic.main.item_help.view.item_help_description
import kotlinx.android.synthetic.main.item_help.view.item_help_title
import kotlinx.android.synthetic.main.item_help.view.item_help_toggle_expand

internal class HelpAdapter(titleDescriptionMap: Map<String, String>) :
  RecyclerView.Adapter<HelpAdapter.Item>() {
  private var titles: Array<String> = titleDescriptionMap.keys.toTypedArray()
  private var descriptions: Array<String> = titleDescriptionMap.values.toTypedArray()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): Item = Item(
    LayoutInflater.from(parent.context)
      .inflate(R.layout.item_help, parent, false)
  )

  override fun onBindViewHolder(
    holder: Item,
    position: Int
  ) {
    holder.itemView.item_help_description.text = descriptions[position]
    holder.itemView.item_help_title.text = titles[position]
  }

  override fun getItemCount(): Int = titles.size

  internal inner class Item(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    init {
      itemView.item_help_title.setOnClickListener { toggleDescriptionVisibility() }
      itemView.item_help_toggle_expand.setOnClickListener { toggleDescriptionVisibility() }
    }

    @SuppressWarnings("MagicNumber")
    fun toggleDescriptionVisibility() {
      if (itemView.item_help_description.visibility == View.GONE) {
        ObjectAnimator.ofFloat(itemView.item_help_toggle_expand, "rotation", 0f, 180f).start()
        itemView.item_help_description.expand()
      } else {
        ObjectAnimator.ofFloat(itemView.item_help_toggle_expand, "rotation", 180f, 360f).start()
        itemView.item_help_description.collapse()
      }
    }
  }
}
