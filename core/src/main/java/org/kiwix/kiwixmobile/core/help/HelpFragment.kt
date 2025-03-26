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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.Navigation
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class HelpFragment : BaseFragment() {
  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  protected abstract val navHostFragmentId: Int

  // Instead of keeping the XML binding, we now directly return a ComposeView.
  protected open fun createFragmentView(
    inflater: LayoutInflater,
    container: ViewGroup?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        // Create the helpScreen data using your rawTitleDescriptionMap.
        val helpScreenData = transformToHelpScreenData(
          requireContext(),
          rawTitleDescriptionMap()
        )
        // Retrieve the NavController if your composable needs it.
        val navController = Navigation.findNavController(requireActivity(), navHostFragmentId)
        // Call your HelpScreen composable.
        HelpScreen(data = helpScreenData, navController = navController)
      }
    }
  }

  // Each subclass is responsible for providing its own raw data.
  protected open fun rawTitleDescriptionMap(): List<Pair<Int, Any>> = emptyList()

  // The following properties are now optional – if no longer use an XML toolbar or title,
  // we can remove or update these accordingly.
  override val fragmentToolbar: Toolbar? by lazy {
    // Already Applied ad TopAppBAr in scaffold in composable
    null
  }
  override val fragmentTitle: String? by lazy { getString(R.string.menu_help) }

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as CoreMainActivity).cachedComponent.inject(this)
  }

  // Remove or adjust onViewCreated if you no longer need to manipulate XML-based views.
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // Any additional logic that is independent of the XML layout can be kept here.
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = createFragmentView(inflater, container)
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
