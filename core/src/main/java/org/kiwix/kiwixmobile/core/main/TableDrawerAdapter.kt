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

package org.kiwix.kiwixmobile.core.main

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.section_list.titleText
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import java.lang.IllegalStateException

class TableDrawerAdapter constructor(private val listener: TableClickListener) :
  Adapter<ViewHolder>() {

  private var title: String = ""
  private var selectedPosition: Int = 0
  private val sections: MutableList<DocumentSection> = mutableListOf()

  fun setSections(sections: List<DocumentSection>) {
    selectedPosition = 0
    this.sections.clear()
    this.sections.addAll(sections)
  }

  fun setTitle(title: String) {
    this.title = title
  }

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) 0
    else 1
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val view = parent.inflate(R.layout.section_list, false)
    if (viewType == 0) return HeaderTableDrawerViewHolder(view)
    return SectionTableDrawerViewHolder(view)
  }

  override fun getItemCount(): Int = sections.size + 1

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    when (holder) {
      is HeaderTableDrawerViewHolder -> {
        holder.bind(title)
        holder.itemView.setOnClickListener { v: View ->
          updateSelection(position)
          listener.onHeaderClick(v)
        }
      }
      is SectionTableDrawerViewHolder -> {
        holder.bind(sections[position - 1])
        holder.itemView.setOnClickListener { v: View? ->
          updateSelection(position)
          listener.onSectionClick(v, selectedPosition - 1)
        }
      }
      else -> {
        throw IllegalStateException("Unknown ViewHolder $holder found")
      }
    }
  }

  private fun updateSelection(position: Int) {
    if (selectedPosition == position) return
    val oldPosition = selectedPosition
    selectedPosition = position
    notifyItemChanged(selectedPosition)
    notifyItemChanged(oldPosition)
  }

  interface TableClickListener {
    fun onHeaderClick(view: View?)
    fun onSectionClick(view: View?, position: Int)
  }

  class HeaderTableDrawerViewHolder(view: View) :
    BaseViewHolder<String>(view) {

    override fun bind(item: String) {
      val context = itemView.context
      titleText.typeface = Typeface.DEFAULT_BOLD
      if (item.isNotEmpty()) titleText.text = item
      else {
        if (context is WebViewProvider) {
          titleText.text =
            context.getCurrentWebView()?.title ?: context.getString(R.string.no_section_info)
        } else titleText.text = context.getString(R.string.no_section_info)
      }
    }
  }

  class SectionTableDrawerViewHolder(view: View) :
    BaseViewHolder<TableDrawerAdapter.DocumentSection>(view) {
    override fun bind(
      item: TableDrawerAdapter.DocumentSection
    ) {
      val context = itemView.context
      val density = context.resources.displayMetrics.density
      val padding = ((item.level - 1) * TableDrawerAdapter.PADDING_ * density).toInt()
      titleText.setPadding(padding, 0, 0, 0)
      titleText.text = item.title
    }
  }

  data class DocumentSection(var title: String, var id: String, var level: Int)

  companion object {
    const val PADDING_ = 16
  }
}
