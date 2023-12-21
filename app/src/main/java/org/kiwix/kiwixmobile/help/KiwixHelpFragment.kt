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

package org.kiwix.kiwixmobile.help

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.help.HelpFragment
import org.kiwix.kiwixmobile.core.main.KIWIX_APK_WEBSITE_URL

class KiwixHelpFragment : HelpFragment() {
  override fun rawTitleDescriptionMap() =
    if (sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
      listOf(
        R.string.help_2 to R.array.description_help_2,
        R.string.help_5 to R.array.description_help_5,
        R.string.how_to_update_content to R.array.update_content_description,
        R.string.zim_files_not_showing to getString(
          R.string.zim_files_not_showing_description,
          KIWIX_APK_WEBSITE_URL
        )
      )
    } else {
      listOf(
        R.string.help_2 to R.array.description_help_2,
        R.string.help_5 to R.array.description_help_5,
        R.string.how_to_update_content to R.array.update_content_description
      )
    }
}
