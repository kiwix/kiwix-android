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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.downloader.downloadManager.APP_NAME_KEY
import org.kiwix.kiwixmobile.core.error.DiagnosticReportActivity
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

abstract class HelpViewModel : ViewModel() {
  abstract suspend fun rawTitleDescriptionMap(context: Context): List<Pair<Int, Any>>
  private val _helpItems: MutableStateFlow<List<HelpScreenItemDataClass>> =
    MutableStateFlow(emptyList())
  val helpItems: StateFlow<List<HelpScreenItemDataClass>> = _helpItems.asStateFlow()

  fun getHelpItems(context: Context) {
    viewModelScope.launch {
      _helpItems.value = transformToHelpScreenData(
        context,
        rawTitleDescriptionMap(context)
      )
    }
  }

  fun onSendReportButtonClick(context: Context) {
    val activity = context as? Activity ?: return
    val appName = (activity as? CoreMainActivity)?.appName
    val intent = Intent(context, DiagnosticReportActivity::class.java)
    val extras = Bundle().apply {
      putString(APP_NAME_KEY, appName)
    }
    intent.putExtras(extras)
    context.startActivity(intent)
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
