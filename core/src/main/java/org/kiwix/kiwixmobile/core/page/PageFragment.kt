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

package org.kiwix.kiwixmobile.core.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.NotesState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

const val SEARCH_ICON_TESTING_TAG = "search"
const val DELETE_MENU_ICON_TESTING_TAG = "deleteMenuIconTestingTag"

abstract class PageFragment : OnItemClickListener, BaseFragment(), FragmentActivityExtensions {
  abstract val pageViewModel: PageViewModel<*, *>

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private var actionMode: ActionMode? = null
  val compositeDisposable = CompositeDisposable()
  abstract val screenTitle: Int
  abstract val noItemsString: String
  abstract val switchString: String
  abstract val searchQueryHint: String
  abstract val pageAdapter: PageAdapter
  abstract val switchIsChecked: Boolean
  abstract val deleteIconTitle: Int
  private val pageState: MutableState<PageState<*>> =
    mutableStateOf(
      NotesState(
        emptyList(),
        true,
        ""
      ),
      policy = referentialEqualityPolicy()
    )

  private val pageScreenState = mutableStateOf(
    // Initial values are empty because this is an abstract class.
    // Before the view is created, the abstract variables have no values.
    // We update this state in `onViewCreated`, once the view is created and the
    // abstract variables are initialized.
    PageFragmentScreenState(
      pageState = pageState.value,
      isSearchActive = false,
      searchQueryHint = "",
      searchText = "",
      searchValueChangedListener = {},
      screenTitle = ZERO,
      noItemsString = "",
      switchString = "",
      switchIsChecked = true,
      switchIsEnabled = true,
      onSwitchCheckedChanged = {},
      deleteIconTitle = ZERO,
      clearSearchButtonClickListener = {}
    )
  )

  private val actionModeCallback: ActionMode.Callback =
    object : ActionMode.Callback {
      override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_context_delete, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

      override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_context_delete) {
          pageViewModel.actions.offer(Action.UserClickedDeleteSelectedPages)
          return true
        }
        pageViewModel.actions.offer(Action.ExitActionModeMenu)
        return false
      }

      override fun onDestroyActionMode(mode: ActionMode) {
        pageViewModel.actions.offer(Action.ExitActionModeMenu)
        actionMode = null
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    pageScreenState.value = pageScreenState.value.copy(
      searchQueryHint = searchQueryHint,
      searchText = "",
      searchValueChangedListener = { onTextChanged(it) },
      clearSearchButtonClickListener = { onTextChanged("") },
      screenTitle = screenTitle,
      noItemsString = noItemsString,
      switchString = switchString,
      switchIsChecked = switchIsChecked,
      onSwitchCheckedChanged = { onSwitchCheckedChanged(it).invoke() },
      deleteIconTitle = deleteIconTitle
    )
    val activity = requireActivity() as CoreMainActivity
    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(activity) })
    pageViewModel.state.observe(viewLifecycleOwner, Observer(::render))
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return ComposeView(requireContext()).apply {
      setContent {
        PageScreen(
          state = pageScreenState.value,
          itemClickListener = this@PageFragment,
          navigationIcon = {
            NavigationIcon(
              onClick = navigationIconClick()
            )
          },
          actionMenuItems = actionMenuList(
            isSearchActive = pageScreenState.value.isSearchActive,
            onSearchClick = {
              // Set the `isSearchActive` when the search button is clicked.
              pageScreenState.value = pageScreenState.value.copy(isSearchActive = true)
            },
            onDeleteClick = { pageViewModel.actions.offer(Action.UserClickedDeleteButton) }
          )
        )
      }
    }
  }

  /**
   * Handles changes to the search text input.
   * - Updates the UI state with the latest search query.
   * - Sends a filter action to the ViewModel to perform search/filtering logic.
   *
   * @param searchText The current text entered in the search bar.
   */
  private fun onTextChanged(searchText: String) {
    pageScreenState.value = pageScreenState.value.copy(searchText = searchText)
    pageViewModel.actions.offer(Action.Filter(searchText))
  }

  /**
   * Returns a lambda to handle switch toggle changes.
   * - Updates the UI state to reflect the new checked status.
   * - Sends an action to the ViewModel to handle the toggle event (e.g., show all items or filter).
   *
   * @param isChecked The new checked state of the switch.
   */
  private fun onSwitchCheckedChanged(isChecked: Boolean): () -> Unit = {
    pageScreenState.value = pageScreenState.value.copy(switchIsChecked = isChecked)
    pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
  }

  /**
   * Handles the click event for the navigation icon.
   * - If search is active, it deactivates the search mode and clears the search text.
   * - Otherwise, it triggers the default back navigation.
   */
  private fun navigationIconClick(): () -> Unit = {
    if (pageScreenState.value.isSearchActive) {
      pageScreenState.value = pageScreenState.value.copy(isSearchActive = false)
      onTextChanged("")
    } else {
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }

  /**
   * Builds the list of action menu items for the app bar.
   *
   * @param isSearchActive Whether the search mode is currently active.
   * @param onSearchClick Callback to invoke when the search icon is clicked.
   * @param onDeleteClick Callback to invoke when the delete icon is clicked.
   * @return A list of [ActionMenuItem]s to be displayed in the app bar.
   *
   * - Shows the search icon only when search is not active.
   * - Always includes the delete icon, with a content description for accessibility (#3825).
   */
  private fun actionMenuList(
    isSearchActive: Boolean,
    onSearchClick: () -> Unit,
    onDeleteClick: () -> Unit
  ): List<ActionMenuItem> {
    return listOfNotNull(
      when {
        !isSearchActive -> ActionMenuItem(
          icon = IconItem.Drawable(R.drawable.action_search),
          contentDescription = R.string.search_label,
          onClick = onSearchClick,
          testingTag = SEARCH_ICON_TESTING_TAG
        )

        else -> null
      },
      ActionMenuItem(
        icon = IconItem.Vector(Icons.Default.Delete),
        // Adding content description for #3825.
        contentDescription = deleteIconTitle,
        onClick = onDeleteClick,
        testingTag = DELETE_MENU_ICON_TESTING_TAG
      )
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun render(state: PageState<*>) {
    pageScreenState.value = pageScreenState.value.copy(
      switchIsEnabled = !state.isInSelectionState,
      // First, assign the existing state to force Compose to recognize a change.
      // This helps when internal properties of items (like `isSelected`) change,
      // but the list reference itself remains the same — Compose won't detect it otherwise.
      pageState = pageState.value
    )
    // Then, assign the actual updated state to trigger full recomposition.
    pageScreenState.value = pageScreenState.value.copy(
      pageState = state
    )
    if (state.isInSelectionState) {
      if (actionMode == null) {
        actionMode =
          (requireActivity() as AppCompatActivity).startSupportActionMode(actionModeCallback)
      }
      actionMode?.title = getString(R.string.selected_items, state.numberOfSelectedItems())
    } else {
      actionMode?.finish()
    }
  }

  override fun onItemClick(page: Page) {
    pageViewModel.actions.offer(Action.OnItemClick(page))
  }

  override fun onItemLongClick(page: Page): Boolean =
    pageViewModel.actions.offer(Action.OnItemLongClick(page))
}
