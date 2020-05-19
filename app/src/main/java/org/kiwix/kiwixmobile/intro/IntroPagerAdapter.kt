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
package org.kiwix.kiwixmobile.intro

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

internal class IntroPagerAdapter(private val views: Array<View>) : PagerAdapter() {
  override fun getCount(): Int = views.size

  override fun instantiateItem(container: ViewGroup, position: Int): Any {
    container.addView(views[position])
    return views[position]
  }

  override fun destroyItem(container: ViewGroup, position: Int, anObject: Any) =
    container.removeView(anObject as View)

  override fun isViewFromObject(view: View, anObject: Any): Boolean = view === anObject
}
