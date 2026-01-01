/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class HelpViewModel @Inject constructor(
  private val kiwixDataStore: KiwixDataStore,
) : ViewModel() {
  private val _helpItems: MutableStateFlow<List<HelpScreenItemDataClass>> =
    MutableStateFlow(emptyList())
  val helpItems: StateFlow<List<HelpScreenItemDataClass>> = _helpItems.asStateFlow()

  fun getHelpItems(context: Context) {
    viewModelScope.launch {
      try {
        val result = kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()

        val rawTitleDescriptionMap = when (result) {
          true -> listOf(
            R.string.help_2 to R.array.description_help_2,
            R.string.help_5 to R.array.description_help_5,
            R.string.how_to_update_content to R.array.update_content_description,
            R.string.why_copy_move_files_to_app_directory to
              context.getString(R.string.copy_move_files_to_app_directory_description)
          )

          false -> listOf(
            R.string.help_2 to R.array.description_help_2,
            R.string.help_5 to R.array.description_help_5,
            R.string.how_to_update_content to R.array.update_content_description,
            R.string.why_copy_move_files_to_app_directory to
              context.getString(R.string.copy_move_files_to_app_directory_description)
          )
        }

        val items = transformToHelpScreenData(context, rawTitleDescriptionMap)
        _helpItems.value = items
      } catch (e: IllegalArgumentException) {
        e.printStackTrace()
      }
    }
  }

  private fun transformToHelpScreenData(
    context: Context,
    rawTitleDescriptionMap: List<Pair<Int, Any>>
  ): List<HelpScreenItemDataClass> {
    return rawTitleDescriptionMap.map { (titleResId, description) ->
      val title = context.getString(titleResId)
      val descriptionValue = when (description) {
        is String -> description
        is Int -> context.resources.getStringArray(description).joinToString(separator = "\n")
        else -> {
          throw IllegalArgumentException("Invalid description resource type for title: $titleResId")
        }
      }
      HelpScreenItemDataClass(title, descriptionValue)
    }
  }
}
