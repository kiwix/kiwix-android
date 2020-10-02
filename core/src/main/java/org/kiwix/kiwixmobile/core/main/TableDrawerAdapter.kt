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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import butterknife.BindView
import butterknife.ButterKnife
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R2

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

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableDrawerViewHolder {
    val res = R.layout.section_list
    val context = parent.context
    val view = LayoutInflater.from(context).inflate(res, parent, false)
    if (viewType == 0) return HeaderTableDrawerViewHolder(view)
    return SectionTableDrawerViewHolder(view)
  }

  override fun getItemCount(): Int = sections.size + 1

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val vh = holder as TableDrawerAdapter.TableDrawerViewHolder
    val context = holder.itemView.context
    vh.itemView.isActivated = holder.adapterPosition == selectedPosition
    when (position) {
      0 -> {
        vh.textViewTitle.typeface = Typeface.DEFAULT_BOLD

        vh.itemView.setOnClickListener { v: View ->
          updateSelection(holder.getAdapterPosition())
          listener.onHeaderClick(v)
        }

        if (title.isNotEmpty()) vh.textViewTitle.text = title
        else {
          var empty = context.getString(R.string.no_section_info)
          if (context is WebViewProvider) {
            empty =
              context.getCurrentWebView()?.title ?: context.getString(R.string.no_section_info)
          }
          vh.textViewTitle.text = empty
        }
      }
      else -> {
        val sectionPosition = position - 1
        val documentSection: DocumentSection = sections[sectionPosition]
        vh.textViewTitle.text = documentSection.title
        val density = context.resources.displayMetrics.density
        val padding = ((documentSection.level - 1) * PADDING_ * density).toInt()
        vh.textViewTitle.setPadding(padding, 0, 0, 0)
        vh.textViewTitle.text = sections[sectionPosition].title
        vh.itemView.setOnClickListener { v: View? ->
          updateSelection(vh.adapterPosition)
          listener.onSectionClick(v!!, sectionPosition)
        }
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

  open inner class TableDrawerViewHolder(v: View) : ViewHolder(v) {
    @BindView(R2.id.titleText)
    lateinit var textViewTitle: TextView

    init {
      ButterKnife.bind(this, v)
    }
  }

  inner class HeaderTableDrawerViewHolder(v: View) : TableDrawerViewHolder(v)

  inner class SectionTableDrawerViewHolder(v: View) : TableDrawerViewHolder(v)

  interface TableClickListener {
    fun onHeaderClick(view: View)
    fun onSectionClick(view: View, position: Int)
  }

  data class DocumentSection(var title: String, var id: String, var level: Int)

  companion object {
    private const val PADDING_ = 16
  }
}
