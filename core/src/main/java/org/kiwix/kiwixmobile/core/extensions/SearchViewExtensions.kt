/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.extensions

import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.setPadding
import org.kiwix.kiwixmobile.core.R

const val CLOSE_ICON_PADDING = 30

fun SearchView.setUpSearchView(context: Context) {
  val heightAndWidth = context.resources.getDimensionPixelSize(
    R.dimen.material_minimum_height_and_width
  )
  val searchViewEditText =
    findViewById<SearchView.SearchAutoComplete>(R.id.search_src_text) as EditText
  val closeImageButton = findViewById<ImageView>(R.id.search_close_btn)
  // set the tooltip on close button to show the description if user long clicks on it.
  TooltipCompat.setTooltipText(
    closeImageButton,
    context.getString(R.string.abc_searchview_description_clear)
  )
  // override the default width of close image button.
  // by default it is 40dp, and the default accessibility width is 48dp
  setWidthWithPadding(
    closeImageButton,
    heightAndWidth,
    CLOSE_ICON_PADDING
  )
  setWrapContentHeightToSearchAutoCompleteEdiText(searchViewEditText)
}

fun setWidthWithPadding(imageView: ImageView, width: Int, padding: Int) {
  imageView.apply {
    val params = layoutParams
    params?.width = width
    setPadding(padding)
    requestLayout()
  }
}

// Set the height to wrap content to this editText to allow expansion of this ediText if needed.
// By default the size of this editText is 36dp which causes the `touch target issue`.
fun setWrapContentHeightToSearchAutoCompleteEdiText(searchEditText: EditText) {
  searchEditText.apply {
    val params = layoutParams
    params?.height = LayoutParams.WRAP_CONTENT
    requestLayout()
  }
}
