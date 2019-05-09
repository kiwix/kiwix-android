/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.zim_manager

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.downloader.DownloadFragment
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment

class SectionsPagerAdapter(
  private val context: Context,
  fm: FragmentManager
) : FragmentPagerAdapter(fm) {

  private val zimFileSelectFragment = ZimFileSelectFragment()
  private val libraryFragment = LibraryFragment()
  private val downloadFragment = DownloadFragment()

  override fun getItem(position: Int) = when (position) {
    0 -> zimFileSelectFragment
    1 -> libraryFragment
    2 -> downloadFragment
    else -> throw  RuntimeException("No matching fragment for position: $position")
  }

  override fun getCount() = 3

  override fun getPageTitle(position: Int) = context.getString(
      when (position) {
        0 -> R.string.local_zims
        1 -> R.string.remote_zims
        2 -> R.string.zim_downloads
        else -> throw  RuntimeException("No matching title for position: $position")
      }
  )
}
