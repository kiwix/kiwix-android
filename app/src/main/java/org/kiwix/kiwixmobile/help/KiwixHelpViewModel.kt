/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.content.Context
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.help.HelpViewModel
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class KiwixHelpViewModel @Inject constructor(
  private val kiwixDataStore: KiwixDataStore
) : HelpViewModel() {
  override suspend fun rawTitleDescriptionMap(context: Context) =
    if (kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()) {
      listOf(
        R.string.help_2 to R.array.description_help_2,
        R.string.help_5 to R.array.description_help_5,
        R.string.how_to_update_content to R.array.update_content_description,
        R.string.why_copy_move_files_to_app_directory to
          context.getString(
            R.string.copy_move_files_to_app_directory_description
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
