package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.BookUtils

class BooksAdapter(
  private val bookUtils: BookUtils,
  private val onItemClick: (Book) -> Unit,
  private val onItemLongClick: (Book) -> Unit
) : RecyclerView.Adapter<BooksViewHolder>() {

  init {
    setHasStableIds(true)
  }

  var itemList: List<Book> = mutableListOf()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  override fun getItemId(position: Int) = itemList[position].databaseId

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ) = BooksViewHolder(parent.inflate(layout.library_item, false), bookUtils)

  override fun getItemCount() = itemList.size

  override fun onBindViewHolder(
    holder: BooksViewHolder,
    position: Int
  ) {
    holder.bind(itemList[position], onItemClick, onItemLongClick)
  }
}