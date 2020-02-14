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

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import kotlin.reflect.KFunction0
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment

class SectionsPagerAdapter(
  private val context: Context,
  private val pagerData: Array<PagerData> =
    arrayOf(
      PagerData(::ZimFileSelectFragment, string.local_zims),
      PagerData(::LibraryFragment, string.remote_zims)
    ),
  fm: FragmentManager
) : FragmentPagerAdapter(fm) {

  override fun getItem(position: Int) = pagerData[position].fragmentConstructor.invoke()

  override fun getCount() = pagerData.size

  override fun getPageTitle(position: Int): String = context.getString(pagerData[position].title)
}

data class PagerData(
  val fragmentConstructor: KFunction0<BaseFragment>,
  val title: Int
)
