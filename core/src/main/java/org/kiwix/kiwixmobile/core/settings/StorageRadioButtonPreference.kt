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
import androidx.preference.CheckBoxPreference
import org.kiwix.kiwixmobile.core.R

class StorageRadioButtonPreference : CheckBoxPreference {
  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  ) {
    setView()
  }

  constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
    setView()
  }

  private fun setView() {
    widgetLayoutResource = R.layout.item_radio_button
  }

  override fun onClick() {
    if (this.isChecked)
      return

    super.onClick()
  }
}
