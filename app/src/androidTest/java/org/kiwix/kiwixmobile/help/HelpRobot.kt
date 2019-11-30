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
package org.kiwix.kiwixmobile.help

import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R.id
import org.kiwix.kiwixmobile.core.R.string

fun help(func: HelpRobot.() -> Unit) = HelpRobot().apply(func)

class HelpRobot : BaseRobot() {

  init {
    isVisible(ViewId(id.activity_help_toolbar))
  }

  fun clickOnWhatDoesKiwixDo() {
    clickOn(TextId(string.help_2))
  }

  fun assertWhatDoesKiwixDoIsExpanded() {
    isVisible(
      Text(
        helpTextFormat(
          string.help_3,
          string.help_4
        )
      )
    )
  }

  fun clickOnWhereIsContent() {
    clickOn(TextId(string.help_5))
  }

  fun assertWhereIsContentIsExpanded() {
    isVisible(
      Text(
        helpTextFormat(
          string.help_6,
          string.help_7,
          string.help_8,
          string.help_9,
          string.help_10,
          string.help_11
        )
      )
    )
  }

  fun clickOnSendFeedback() {
    clickOn(ViewId(id.activity_help_feedback_text_view))
  }

  private fun helpTextFormat(vararg stringIds: Int) =
    stringIds.fold("", { acc, i -> "$acc${context.getString(i)}\n" })
}
