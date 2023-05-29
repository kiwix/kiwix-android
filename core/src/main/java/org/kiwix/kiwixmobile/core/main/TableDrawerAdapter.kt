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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.core.databinding.SectionListBinding

class TableDrawerAdapter constructor(private val listener: TableClickListener) :
  Adapter<ViewHolder>() {

  private var title: String = ""
  private val sections: MutableList<DocumentSection> = mutableListOf()

  fun setSections(sections: List<DocumentSection>) {
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
    val binding = SectionListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return if (viewType == 0) {
      HeaderTableDrawerViewHolder(binding)
    } else {
      SectionTableDrawerViewHolder(binding)
    }
  }

  override fun getItemCount(): Int = sections.size + 1

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    when (holder) {
      is HeaderTableDrawerViewHolder -> {
        holder.bind(title)
        holder.itemView.setOnClickListener(listener::onHeaderClick)
      }
      is SectionTableDrawerViewHolder -> {
        val titleAdjustedPosition = position - 1
        holder.bind(sections[titleAdjustedPosition])
        holder.itemView.setOnClickListener {
          listener.onSectionClick(it, titleAdjustedPosition)
        }
      }
      else -> {
        throw IllegalStateException("Unknown ViewHolder $holder found")
      }
    }
  }

  interface TableClickListener {
    fun onHeaderClick(view: View?)
    fun onSectionClick(view: View?, position: Int)
  }

  class HeaderTableDrawerViewHolder(private val sectionListBinding: SectionListBinding) :
    BaseViewHolder<String>(sectionListBinding.root) {

    override fun bind(item: String) {
      val context = itemView.context
      sectionListBinding.titleText.typeface = Typeface.DEFAULT_BOLD
      sectionListBinding.titleText.text = when {
        item.isNotEmpty() -> item
        context is WebViewProvider -> context.getCurrentWebView()?.title
          ?: context.getString(R.string.no_section_info)
        else -> context.getString(R.string.no_section_info)
      }
    }
  }

  class SectionTableDrawerViewHolder(private val sectionListBinding: SectionListBinding) :
    BaseViewHolder<TableDrawerAdapter.DocumentSection>(sectionListBinding.root) {
    override fun bind(
      item: TableDrawerAdapter.DocumentSection
    ) {
      val context = itemView.context
      val padding =
        ((item.level - 1) * context.resources.getDimension(R.dimen.title_text_padding)).toInt()
      sectionListBinding.titleText.setPadding(padding, 0, 0, 0)
      sectionListBinding.titleText.text = item.title
    }
  }

  data class DocumentSection(var title: String, var id: String, var level: Int)
}
