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
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.databinding.FragmentPageBinding
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.setUpSearchView
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
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
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
  private var fragmentPageBinding: FragmentPageBinding? = null

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

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.menu_page, menu)
          val search = menu.findItem(R.id.menu_page_search).actionView as SearchView
          search.apply {
            setUpSearchView(requireActivity())
            queryHint = searchQueryHint
            setOnQueryTextListener(
              SimpleTextListener { query, _ ->
                pageViewModel.actions.offer(Action.Filter(query))
              }
            )
          }
          // menu.findItem(R.id.menu_pages_clear).title = deleteIconTitle // Bug fix #3825
        }

        @Suppress("ReturnCount")
        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            android.R.id.home -> {
              pageViewModel.actions.offer(Action.Exit)
              return true
            }

            R.id.menu_pages_clear -> {
              pageViewModel.actions.offer(Action.UserClickedDeleteButton)
              return true
            }
          }
          return false
        }
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Log.e("ON_VIEW_CREATED", "onViewCreated: $screenTitle")
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

    // hides keyboard when scrolled
    fragmentPageBinding?.recyclerView?.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          fragmentPageBinding?.recyclerView?.closeKeyboard()
        }
      }
    )
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
              iconItem = navigationIconItem(pageScreenState.value.isSearchActive),
              onClick = navigationIconClick()
            )
          },
          actionMenuItems = actionMenuList(
            isSearchActive = pageScreenState.value.isSearchActive,
            onSearchClick = {
              pageScreenState.value = pageScreenState.value.copy(isSearchActive = true)
            },
            onDeleteClick = { pageViewModel.actions.offer(Action.UserClickedDeleteButton) }
          )
        )
      }
    }
  }

  private fun onTextChanged(searchText: String) {
    pageScreenState.value = pageScreenState.value.copy(searchText = searchText)
    pageViewModel.actions.offer(Action.Filter(searchText))
  }

  private fun onSwitchCheckedChanged(isChecked: Boolean): () -> Unit = {
    pageScreenState.value = pageScreenState.value.copy(switchIsChecked = isChecked)
    pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
  }

  private fun navigationIconItem(isSearchActive: Boolean) = if (isSearchActive) {
    IconItem.Drawable(R.drawable.ic_close_white_24dp)
  } else {
    IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
  }

  private fun navigationIconClick(): () -> Unit = {
    if (pageScreenState.value.isSearchActive) {
      pageScreenState.value = pageScreenState.value.copy(isSearchActive = false)
      pageViewModel.actions.offer(Action.Exit)
    } else {
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }

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
