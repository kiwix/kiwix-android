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

package org.kiwix.kiwixmobile.custom.help

import org.kiwix.kiwixmobile.core.help.HelpFragment

class CustomHelpFragment : HelpFragment() {
  override val navHostFragmentId: Int
    get() = org.kiwix.kiwixmobile.custom.R.id.custom_nav_controller

  override fun rawTitleDescriptionMap() =
    if (sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
      listOf(
        org.kiwix.kiwixmobile.core.R.string.help_2 to
          org.kiwix.kiwixmobile.core.R.array.description_help_2,
        org.kiwix.kiwixmobile.core.R.string.help_5 to
          org.kiwix.kiwixmobile.core.R.array.description_help_5,
        org.kiwix.kiwixmobile.core.R.string.how_to_update_content to
          org.kiwix.kiwixmobile.core.R.array.update_content_description,
        org.kiwix.kiwixmobile.core.R.string.why_copy_move_files_to_app_directory to
          getString(
            org.kiwix.kiwixmobile.core.R.string.copy_move_files_to_app_directory_description
          )
      )
    } else {
      listOf(
        org.kiwix.kiwixmobile.core.R.string.help_2 to
          org.kiwix.kiwixmobile.core.R.array.description_help_2,
        org.kiwix.kiwixmobile.core.R.string.help_5 to
          org.kiwix.kiwixmobile.core.R.array.description_help_5,
        org.kiwix.kiwixmobile.core.R.string.how_to_update_content to
          org.kiwix.kiwixmobile.core.R.array.update_content_description
      )
    }
}
