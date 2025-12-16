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
package org.kiwix.kiwixmobile.core.help

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.downloader.downloadManager.APP_NAME_KEY
import org.kiwix.kiwixmobile.core.error.DiagnosticReportActivity
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class HelpFragment : BaseFragment() {
  @Inject
  lateinit var kiwixDataStore: KiwixDataStore

  private val helpScreenData = mutableStateListOf<HelpScreenItemDataClass>()

  // Each subclass is responsible for providing its own raw data.
  protected open suspend fun rawTitleDescriptionMap(): List<Pair<Int, Any>> = emptyList()

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as CoreMainActivity).cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).apply {
    setContent {
      LaunchedEffect(Unit) {
        // Create the helpScreen data using your rawTitleDescriptionMap.
        helpScreenData.clear()
        helpScreenData.addAll(
          transformToHelpScreenData(
            requireContext(),
            rawTitleDescriptionMap()
          )
        )
      }
      // Call your HelpScreen composable.
      HelpScreen(data = helpScreenData, { onSendReportButtonClick() }) {
        NavigationIcon(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() })
      }
    }
  }

  private fun onSendReportButtonClick() {
    if (activity == null) return
    val appName = (activity as? CoreMainActivity)?.appName
    val intent = Intent(activity, DiagnosticReportActivity::class.java)
    val extras = Bundle()
    extras.putString(APP_NAME_KEY, appName)
    intent.putExtras(extras)
    activity?.startActivity(intent)
  }
}

// Util function to modify the data accordingly
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
