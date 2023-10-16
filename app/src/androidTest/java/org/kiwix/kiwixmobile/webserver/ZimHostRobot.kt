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

package org.kiwix.kiwixmobile.webserver

import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.R

fun zimHost(func: ZimHostRobot.() -> Unit) = ZimHostRobot().applyWithViewHierarchyPrinting(func)

class ZimHostRobot : BaseRobot() {

  fun assertMenuWifiHotspotDiplayed() {
    isVisible(TextId(R.string.menu_wifi_hotspot))
  }

  fun refreshLibraryList() {
    refresh(R.id.zim_swiperefresh)
  }

  fun assertZimFilesLoaded() {
    isVisible(Text("Test_Zim"))
  }
}
