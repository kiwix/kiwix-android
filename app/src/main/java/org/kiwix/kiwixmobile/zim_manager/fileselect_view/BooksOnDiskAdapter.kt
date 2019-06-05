package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.utils.BookUtils

class BooksOnDiskAdapter(
  private val bookUtils: BookUtils,
  private val onItemClick: (BookOnDisk) -> Unit,
  private val onItemLongClick: (BookOnDisk) -> Unit
) : RecyclerView.Adapter<BooksOnDiskViewHolder>() {

  init {
    setHasStableIds(true)
  }

  var itemList: List<BookOnDisk> = mutableListOf()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun getItemId(position: Int) = itemList[position].databaseId!!

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = BooksOnDiskViewHolder(parent.inflate(R.layout.library_item, false), bookUtils)

  override fun getItemCount() = itemList.size

  override fun onBindViewHolder(
    holder: BooksOnDiskViewHolder,
    position: Int
  ) {
    holder.bind(itemList[position], onItemClick, onItemLongClick)
  }
}
