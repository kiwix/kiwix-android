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
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.END
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.START
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.getAttribute
import org.kiwix.kiwixmobile.core.extensions.setImageDrawableCompat
import org.kiwix.kiwixmobile.core.extensions.setToolTipWithContentDescription
import org.kiwix.kiwixmobile.core.extensions.tint
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getToolbarHeight
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getWindowHeight
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getWindowWidth
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml

class TabsAdapter internal constructor(
  private val activity: AppCompatActivity,
  private val webViews: List<KiwixWebView>,
  private val painter: NightModeViewPainter
) : RecyclerView.Adapter<TabsAdapter.ViewHolder>() {

  init {
    setHasStableIds(true)
  }

  private var listener: TabClickListener? = null
  var selected = 0
  @SuppressLint("ResourceType") override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val context = parent.context
    val margin16 = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
    val closeImageWidthAndHeight =
      context.resources.getDimensionPixelSize(R.dimen.close_tab_button_size)
    val close = ImageView(context)
      .apply {
        id = R.id.tabsAdapterCloseImageView
        setImageDrawableCompat(R.drawable.ic_clear_white_24dp)
        setToolTipWithContentDescription(resources.getString(R.string.close_tab))
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.actionBarItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
        tint(context.getAttribute(R.attr.colorOnSurface))
      }
    val cardView = MaterialCardView(context)
      .apply {
        id = R.id.tabsAdapterCardView
        useCompatPadding = true
      }
    val textView = TextView(context)
      .apply {
        id = R.id.tabsAdapterTextView
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
      }
    val constraintLayout = ConstraintLayout(context)
      .apply {
        isFocusableInTouchMode = true
        addView(
          cardView,
          ConstraintLayout.LayoutParams(
            activity.getWindowWidth() / 2,
            -activity.getToolbarHeight() / 2 + activity.getWindowHeight() / 2
          )
        )
        addView(
          close,
          ConstraintLayout.LayoutParams(closeImageWidthAndHeight, closeImageWidthAndHeight)
        )
        layoutParams = RecyclerView.LayoutParams(
          RecyclerView.LayoutParams.WRAP_CONTENT,
          RecyclerView.LayoutParams.MATCH_PARENT
        )
        addView(
          textView,
          ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        )
      }
    ConstraintSet()
      .apply {
        clone(constraintLayout)
        connect(cardView.id, TOP, PARENT_ID, TOP)
        connect(cardView.id, BOTTOM, PARENT_ID, BOTTOM)
        connect(cardView.id, START, PARENT_ID, START, margin16)
        connect(cardView.id, END, PARENT_ID, END, margin16)
        connect(close.id, END, cardView.id, END)
        connect(close.id, BOTTOM, cardView.id, TOP)
        connect(textView.id, BOTTOM, cardView.id, TOP)
        connect(textView.id, START, cardView.id, START, margin16 / 8)
        connect(textView.id, END, close.id, START)
        applyTo(constraintLayout)
      }
    return ViewHolder(constraintLayout, textView, close, cardView)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val webView = webViews[position]
    webView.parent?.let { (it as ViewGroup).removeView(webView) }
    val webViewTitle = webView.title.fromHtml().toString()
    holder.apply {
      title.text = webViewTitle
      close.setOnClickListener { v: View -> listener?.onCloseTab(v, adapterPosition) }
      materialCardView.apply {
        removeAllViews()
        // Create a new FrameLayout to hold the web view and custom view
        val frameLayout = FrameLayout(context)
        // Add the web view to the frame layout
        frameLayout.addView(webView)
        // Create a custom view that covers the entire
        // webView(which prevent to clicks inside the webView) and handles tab selection
        val view = View(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
          setOnClickListener { v: View ->
            selected = adapterPosition
            listener?.onSelectTab(v, selected)
            notifyDataSetChanged()
          }
        }
        // Add the custom view to the frame layout
        frameLayout.addView(view)
        // Add the frame layout to the material card view
        addView(
          frameLayout,
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        )
      }
    }
    if (webViewTitle != activity.getString(R.string.menu_home)) {
      painter.update(webView) // if the webView is not opened yet
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

  class ViewHolder(
    view: View,
    val title: TextView,
    val close: ImageView,
    val materialCardView: MaterialCardView
  ) :
    RecyclerView.ViewHolder(view)
}
