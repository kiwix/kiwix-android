package org.kiwix.kiwixmobile.views

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language

class LanguageAdapter(val listItems: MutableList<Language>) : RecyclerView.Adapter<LanguageViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = LanguageViewHolder(
      parent.inflate(R.layout.language_check_item, false),
      this::toggleItemAt
  )

  override fun getItemCount() = listItems.size

  override fun onBindViewHolder(
    holder: LanguageViewHolder,
    position: Int
  ) {
    holder.bind(listItems[position], position)
  }

  private fun toggleItemAt(position: Int) {
    listItems[position] = listItems[position].also { it.active = !it.active }
    notifyItemChanged(position)
  }
}

