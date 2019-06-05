package org.kiwix.kiwixmobile.views


import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language

class LanguageAdapter(val listItems: MutableList<Language>) : RecyclerView.Adapter<LanguageViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = LanguageViewHolder(
      parent.inflate(R.layout.item_language, false),
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

