package org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.extensions.LayoutContainer

abstract class BaseViewHolder<in ITEM>(override val containerView: View) : ViewHolder(
  containerView
), LayoutContainer {
  abstract fun bind(item: ITEM)
}
