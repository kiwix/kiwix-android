/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.note

import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable
import org.kiwix.kiwixmobile.core.R

fun note(func: NoteRobot.() -> Unit) = NoteRobot().apply(func)

class NoteRobot : BaseRobot() {

  init {
    isVisible(Findable.ViewId(R.id.toolbar))
  }

  fun assertNoteRecyclerViewExist() {
    isVisible(Findable.ViewId(R.id.recycler_view))
  }

  fun assertSwitchWidgetExist() {
    isVisible(Findable.ViewId(R.id.page_switch))
  }
}
