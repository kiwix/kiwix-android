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

package org.kiwix.kiwixmobile.language

import android.os.Bundle
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import javax.inject.Inject

const val SAVE_ICON_TESTING_TAG = "saveLanguages"

class LanguageFragment : BaseFragment() {
  private val languageViewModel by lazy { viewModel<LanguageViewModel>(viewModelFactory) }

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private var composeView: ComposeView? = null

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val activity = requireActivity() as CoreMainActivity
    composeView?.setContent {
      var searchText by remember { mutableStateOf("") }
      var isSearchActive by remember { mutableStateOf(false) }

      // fun resetSearchState() {
      //   // clears the search text and resets the filter
      //   searchText = ""
      //   languageViewModel.actions.tryEmit(Action.Filter(searchText))
      // }
      //
      // KiwixTheme {
      //   LanguageScreen(
      //     searchText = searchText,
      //     isSearchActive = isSearchActive,
      //     languageViewModel = languageViewModel,
      //     actionMenuItemList = appBarActionMenuList(
      //       isSearchActive = isSearchActive,
      //       onSearchClick = { isSearchActive = true },
      //       onSaveClick = {
      //         languageViewModel.actions.tryEmit(Action.Save)
      //       }
      //     ),
      //     onClearClick = { resetSearchState() },
      //     onAppBarValueChange = {
      //       searchText = it
      //       languageViewModel.actions.tryEmit(Action.Filter(it.trim()))
      //     },
      //     navigationIcon = {
      //       NavigationIcon(
      //         iconItem = if (isSearchActive) {
      //           IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
      //         } else {
      //           IconItem.Drawable(
      //             R.drawable.ic_close_white_24dp
      //           )
      //         },
      //         onClick = {
      //           if (isSearchActive) {
      //             isSearchActive = false
      //             resetSearchState()
      //           } else {
      //             activity.onBackPressedDispatcher.onBackPressed()
      //           }
      //         }
      //       )
      //     }
      //   )
    }
  }

  private fun appBarActionMenuList(
    isSearchActive: Boolean,
    onSearchClick: () -> Unit,
    onSaveClick: () -> Unit
  ): List<ActionMenuItem> {
    return listOfNotNull(
      when {
        !isSearchActive -> ActionMenuItem(
          icon = IconItem.Drawable(R.drawable.action_search),
          contentDescription = R.string.search_label,
          onClick = onSearchClick,
          testingTag = SEARCH_ICON_TESTING_TAG
        )

        else -> null // Handle the case when both conditions are false
      },
      // Second item: always included
      ActionMenuItem(
        icon = IconItem.Vector(Icons.Default.Check),
        contentDescription = R.string.save_languages,
        onClick = onSaveClick,
        testingTag = SAVE_ICON_TESTING_TAG
      )
    )

// override fun onCreateView(
//   inflater: LayoutInflater,
//   container: ViewGroup?,
//   savedInstanceState: Bundle?
// ): View {
//   return ComposeView(requireContext()).also {
//     composeView = it
//   }
// }
//
// override fun onDestroyView() {
//   super.onDestroyView()
//   composeView?.disposeComposition()
//   composeView = null
// }
  }
}
