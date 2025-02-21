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

package org.kiwix.kiwixmobile.core.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_EXTERNAL_STORAGE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.PREF_INTERNAL_STORAGE

class StorageRadioButtonPreference @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : CheckBoxPreference(context, attrs, defStyleAttr) {
  init {
    widgetLayoutResource = R.layout.item_storage_preference
  }

  private var radioButton: RadioButton? = null
  private var progressBar: ProgressBar? = null
  private var usedSpaceTextView: TextView? = null
  private var freeSpaceTextView: TextView? = null
  private var pathAndTitleTextView: TextView? = null
  private var usedSpace: String? = null
  private var freeSpace: String? = null
  private var pathAndTitle: String? = null
  private var progress: Int = 0

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    radioButton = holder.findViewById(R.id.radioButton) as RadioButton
    progressBar = holder.findViewById(R.id.storageProgressBar) as ProgressBar
    usedSpaceTextView = holder.findViewById(R.id.usedSpace) as TextView
    freeSpaceTextView = holder.findViewById(R.id.freeSpace) as TextView
    pathAndTitleTextView = holder.findViewById(R.id.storagePathAndTitle) as TextView
    radioButton?.isChecked = isChecked

    usedSpaceTextView?.let { it.text = usedSpace }
    freeSpaceTextView?.let { it.text = freeSpace }
    pathAndTitleTextView?.let { it.text = pathAndTitle }
    progressBar?.let { it.progress = progress }
  }

  override fun onClick() {
    if (isChecked) return
    preferenceManager.findPreference<CheckBoxPreference>(PREF_INTERNAL_STORAGE)?.isChecked = false
    preferenceManager.findPreference<CheckBoxPreference>(PREF_EXTERNAL_STORAGE)?.isChecked = false
    super.onClick()
  }

  fun setProgress(usedPercentage: Int) {
    progress = usedPercentage
    progressBar?.progress = usedPercentage
  }

  fun setUsedSpace(usedSpace: String) {
    this.usedSpace = usedSpace
    usedSpaceTextView?.text = usedSpace
  }

  fun setFreeSpace(freeSpace: String) {
    this.freeSpace = freeSpace
    freeSpaceTextView?.text = freeSpace
  }

  fun setPathAndTitleForStorage(storageTitleAndPath: String) {
    pathAndTitle = storageTitleAndPath
    pathAndTitleTextView?.text = storageTitleAndPath
  }
}
