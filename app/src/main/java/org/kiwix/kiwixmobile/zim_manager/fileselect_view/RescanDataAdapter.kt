package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.extensions.inflate
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.utils.BookUtils

// The Adapter for the ListView for when the ListView is populated with the rescanned files
public class RescanDataAdapter(
  val bookUtils: BookUtils,
  val onItemClick: (Book) -> Unit,
  val onItemLongClick: (Book) -> Unit
) : RecyclerView.Adapter<RescanViewHolder>() {

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
  ) = RescanViewHolder(parent.inflate(layout.library_item, false), bookUtils)

  override fun getItemCount() = itemList.size

  override fun onBindViewHolder(
    holder: RescanViewHolder,
    position: Int
  ) {
    holder.bind(itemList[position], onItemClick, onItemLongClick)
  }
}