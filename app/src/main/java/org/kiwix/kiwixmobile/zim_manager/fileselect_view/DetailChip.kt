/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.detail_chip.view.detail
import kotlinx.android.synthetic.main.detail_chip.view.label
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate

class DetailChip(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

  init {
    orientation = HORIZONTAL
    inflate(R.layout.detail_chip, true)
    withTypedArray(attrs, R.styleable.DetailChip) {
      label.text =
        context.getString(
          R.string.detail_label_format,
          getString(R.styleable.DetailChip_label)
            ?: throw NullPointerException("No value set for label")
        )
    }
    if (isInEditMode) {
      detail.text = "Short text"
    }
  }

  fun setDetail(detailString: String?) {
    if (detailString != null)
      detail.text = detailString
    else
      visibility = View.GONE
  }
}

private fun View.withTypedArray(
  attrs: AttributeSet,
  styleable: IntArray,
  typedArrayFunc: TypedArray.() -> Unit
) {
  context.theme.obtainStyledAttributes(attrs, styleable, 0, 0).let {
    try {
      typedArrayFunc(it)
    } finally {
      it.recycle()
    }
  }
}
