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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.help.HelpScreen
import org.kiwix.kiwixmobile.core.help.HelpScreenItemDataClass
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

open class KiwixHelpFragment : Fragment() {
  private lateinit var navController: NavController
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val context = requireContext()
    val sharedPrefUtil = SharedPreferenceUtil(context)

    val rawData = if (sharedPrefUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
      listOf(
        org.kiwix.kiwixmobile.core.R.string.help_2 to org.kiwix.kiwixmobile.core.R.array
          .description_help_2,
        org.kiwix.kiwixmobile.core.R.string.help_5 to org.kiwix.kiwixmobile.core.R.array
          .description_help_5,
        org.kiwix.kiwixmobile.core.R.string.how_to_update_content to org.kiwix.kiwixmobile
          .core.R.array.update_content_description,
        org.kiwix.kiwixmobile.core.R.string.why_copy_move_files_to_app_directory to getString(
          org.kiwix.kiwixmobile.core.R.string.copy_move_files_to_app_directory_description
        )
      )
    } else {
      listOf(
        org.kiwix.kiwixmobile.core.R.string.help_2 to org.kiwix.kiwixmobile.core.R.array
          .description_help_2,
        org.kiwix.kiwixmobile.core.R.string.help_5 to org.kiwix.kiwixmobile.core.R.array
          .description_help_5,
        org.kiwix.kiwixmobile.core.R.string.how_to_update_content to org.kiwix.kiwixmobile
          .core.R.array.update_content_description
      )
    }

    val helpScreenData: List<HelpScreenItemDataClass> = transformToHelpScreenData(context, rawData)
    navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)

    return ComposeView(requireContext()).apply {
      setContent {
        HelpScreen(data = helpScreenData, navController = navController)
      }
    }
  }
}

fun transformToHelpScreenData(
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
