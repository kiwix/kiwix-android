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

package org.kiwix.kiwixmobile.nav.destination.library

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.language.LanguageRobot
import org.kiwix.kiwixmobile.language.language

/**
 * Authored by Ayush Shrivastava on 4/11/20
 */

fun onlineLibrary(func: OnlineLibraryRobot.() -> Unit) =
  OnlineLibraryRobot().applyWithViewHierarchyPrinting(func)

class OnlineLibraryRobot : BaseRobot() {
  init {
    isVisible(Findable.ViewId(R.id.libraryList))
  }

  fun clickOnGlobeIcon(func: LanguageRobot.() -> Unit) {
    clickOn(Findable.ViewId(R.id.select_language))
    isVisible(TextId(R.string.select_languages))
    language(func)
  }
}
