/*
 * Kiwix Android
 * Copyright (c) 2O24 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View.VISIBLE
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO

const val MARGIN_TOP = 8

class StorageLoadingPreference @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = ZERO
) : Preference(context, attrs, defStyleAttr) {

  private var customProgressTitle: TextView? = null
  private var progressBarTitleText: String? = null

  init {
    widgetLayoutResource = R.layout.item_custom_spinner
  }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val progressBar = holder.findViewById(R.id.custom_progressbar) as? ProgressBar
    customProgressTitle = holder.findViewById(R.id.custom_progress_title) as TextView
    progressBarTitleText?.let(::setProgressBarTitle)

    val constraintLayout = holder.itemView as ConstraintLayout
    val constraintSet = ConstraintSet()
    constraintSet.clone(constraintLayout)

    constraintSet.connect(
      progressBar?.id ?: ZERO,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
      ZERO
    )
    constraintSet.connect(
      progressBar?.id ?: ZERO,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
      ZERO
    )
    constraintSet.connect(
      progressBar?.id ?: ZERO,
      ConstraintSet.TOP,
      ConstraintSet.PARENT_ID,
      ConstraintSet.TOP,
      ZERO
    )

    constraintSet.connect(
      customProgressTitle?.id ?: ZERO,
      ConstraintSet.START,
      ConstraintSet.PARENT_ID,
      ConstraintSet.START,
      ZERO
    )
    constraintSet.connect(
      customProgressTitle?.id ?: ZERO,
      ConstraintSet.END,
      ConstraintSet.PARENT_ID,
      ConstraintSet.END,
      ZERO
    )
    constraintSet.connect(
      customProgressTitle?.id ?: ZERO,
      ConstraintSet.TOP,
      progressBar?.id ?: ZERO,
      ConstraintSet.BOTTOM,
      MARGIN_TOP
    )
    constraintSet.connect(
      customProgressTitle?.id ?: ZERO,
      ConstraintSet.BOTTOM,
      ConstraintSet.PARENT_ID,
      ConstraintSet.BOTTOM,
      ZERO
    )
    constraintSet.applyTo(constraintLayout)
  }

  fun setTitle(title: String) {
    progressBarTitleText = title
    setProgressBarTitle(title)
  }

  private fun setProgressBarTitle(title: String) {
    customProgressTitle?.apply {
      text = title
      visibility = VISIBLE
    }
  }
}
