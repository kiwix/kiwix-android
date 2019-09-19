package org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter

import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.AdapterDelegate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.BaseDelegateAdapter

class BooksOnDiskAdapter(
  vararg delegates: AdapterDelegate<BooksOnDiskListItem>
) : BaseDelegateAdapter<BooksOnDiskListItem>(
  *delegates
) {
  override fun getIdFor(item: BooksOnDiskListItem) = item.id
}
