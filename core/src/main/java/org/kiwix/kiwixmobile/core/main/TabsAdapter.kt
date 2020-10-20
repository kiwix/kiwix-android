/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.getAttribute
import org.kiwix.kiwixmobile.core.extensions.tint
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getToolbarHeight
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getWindowHeight
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getWindowWidth
import org.kiwix.kiwixmobile.core.utils.ImageUtils.getBitmapFromView
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml

class TabsAdapter internal constructor(
  private val activity: AppCompatActivity,
  private val webViews: List<KiwixWebView>,
  private val painter: NightModeViewPainter
) : RecyclerView.Adapter<TabsAdapter.ViewHolder>() {
  private var listener: TabClickListener? = null
  var selected = 0
  @SuppressLint("ResourceType") override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val context = parent.context
    val margin16 = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
    val contentImage = ImageView(context)
    contentImage.id = 1
    contentImage.scaleType = ImageView.ScaleType.FIT_XY
    val close = ImageView(context)
    close.id = 2
    close.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_clear_white_24dp))
    close.tint(context.getAttribute(R.attr.colorOnSurface))
    val cardView = MaterialCardView(context)
    cardView.id = 3
    cardView.useCompatPadding = true
    cardView.addView(
      contentImage,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    )
    val constraintLayout = ConstraintLayout(context)
    constraintLayout.isFocusableInTouchMode = true
    constraintLayout.addView(
      cardView,
      ConstraintLayout.LayoutParams(
        activity.getWindowWidth() / 2,
        -activity.getToolbarHeight() / 2 + activity.getWindowHeight() / 2
      )
    )
    constraintLayout.addView(
      close, ConstraintLayout.LayoutParams(
        margin16,
        margin16
      )
    )
    constraintLayout.layoutParams = RecyclerView.LayoutParams(
      RecyclerView.LayoutParams.WRAP_CONTENT,
      RecyclerView.LayoutParams.MATCH_PARENT
    )
    val textView = TextView(context)
    textView.id = 4
    textView.maxLines = 1
    textView.ellipsize = TextUtils.TruncateAt.END
    constraintLayout.addView(
      textView,
      ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
    )
    val constraintSet = ConstraintSet()
    constraintSet.clone(constraintLayout)
    constraintSet.connect(
      cardView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID,
      ConstraintSet.TOP
    )
    constraintSet.connect(
      cardView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM
    )
    constraintSet.connect(
      cardView.id, ConstraintSet.START, ConstraintSet.PARENT_ID,
      ConstraintSet.START, margin16
    )
    constraintSet.connect(
      cardView.id, ConstraintSet.END, ConstraintSet.PARENT_ID,
      ConstraintSet.END, margin16
    )
    constraintSet.connect(close.id, ConstraintSet.END, cardView.id, ConstraintSet.END)
    constraintSet.connect(close.id, ConstraintSet.BOTTOM, cardView.id, ConstraintSet.TOP)
    constraintSet.connect(
      textView.id, ConstraintSet.BOTTOM, cardView.id,
      ConstraintSet.TOP
    )
    constraintSet.connect(
      textView.id, ConstraintSet.START, cardView.id,
      ConstraintSet.START, margin16 / 8
    )
    constraintSet.connect(textView.id, ConstraintSet.END, close.id, ConstraintSet.START)
    constraintSet.applyTo(constraintLayout)
    return ViewHolder(constraintLayout, contentImage, textView, close)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val webView = webViews[position]
    webView.parent?.let { (it as ViewGroup).removeView(webView) }
    val webViewTitle = webView.title.fromHtml().toString()
    holder.title.text = webViewTitle
    holder.close.setOnClickListener { v: View -> listener?.onCloseTab(v, holder.adapterPosition) }
    holder.content.setImageBitmap(
      getBitmapFromView(webView, activity.getWindowWidth(), activity.getWindowHeight())
    )
    holder.content.setOnClickListener { v: View ->
      selected = holder.adapterPosition
      listener?.onSelectTab(v, selected)
      notifyDataSetChanged()
    }
    if (webViewTitle != activity.getString(R.string.menu_home)) {
      painter.update(holder.content)
    }
  }

  override fun getItemCount(): Int = webViews.size

  override fun getItemId(position: Int): Long = webViews[position].hashCode().toLong()

  fun setTabClickListener(listener: TabClickListener) {
    this.listener = listener
  }

  interface TabClickListener {
    fun onSelectTab(view: View, position: Int)
    fun onCloseTab(view: View, position: Int)
  }

  class ViewHolder(view: View, val content: ImageView, val title: TextView, val close: ImageView) :
    RecyclerView.ViewHolder(view)

  init {
    setHasStableIds(true)
  }
}
