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

package org.kiwix.kiwixmobile.zim_manager

import androidx.viewpager.widget.ViewPager.OnPageChangeListener

class SimplePageChangeListener constructor(
  private val onPageSelectedAction: (Int) -> Unit,
  private val onPageScrollStateChanged: ((Int) -> Unit)? = null
) : OnPageChangeListener {
  override fun onPageScrolled(
    position: Int,
    positionOffset: Float,
    positionOffsetPixels: Int
  ) {
  }

  override fun onPageSelected(position: Int) {
    onPageSelectedAction.invoke(position)
  }

  override fun onPageScrollStateChanged(state: Int) {
    onPageScrollStateChanged?.invoke(state)
  }
}
