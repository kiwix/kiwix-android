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
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.databinding.FragmentPageBinding
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.setToolTipWithContentDescription
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

  /**
   * Controls the visibility of the "Switch", and its controls.
   *
   * A [Triple] containing:
   *  - [String]: The text displayed with switch.
   *  - [Boolean]: Whether the switch is checked or not.
   *  - [Boolean]: Whether the switch is enabled or disabled.
   */
  private val pageSwitchItem = mutableStateOf(Triple("", true, true))
  private var fragmentPageBinding: FragmentPageBinding? = null
  override val fragmentToolbar: Toolbar? by lazy {
    fragmentPageBinding?.root?.findViewById(R.id.toolbar)
  }

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
    // setupMenu()
    val activity = requireActivity() as CoreMainActivity
    fragmentPageBinding?.recyclerView?.apply {
      layoutManager =
        LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
      adapter = pageAdapter
      fragmentTitle?.let(::setToolTipWithContentDescription)
    }
    fragmentPageBinding?.noPage?.text = noItemsString

    // fragmentPageBinding?.pageSwitch?.apply {
    //   text = switchString
    //   isChecked = switchIsChecked
    //   // hide switches for custom apps, see more info here https://github.com/kiwix/kiwix-android/issues/3523
    //   visibility = if (requireActivity().isCustomApp()) GONE else VISIBLE
    // }
    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(activity) })
    // fragmentPageBinding?.pageSwitch?.setOnCheckedChangeListener { _, isChecked ->
    //   pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    // }
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
        val isSearchActive = remember { mutableStateOf(false) }
        PageScreen(
          pageState = pageState.value,
          pageSwitchItem = pageSwitchItem.value,
          screenTitle = screenTitle,
          noItemsString = noItemsString,
          searchQueryHint = searchQueryHint,
          onSwitchChanged = { onSwitchCheckedChanged(it) },
          itemClickListener = this@PageFragment,
          navigationIcon = {
            NavigationIcon(
              iconItem = navigationIconItem(isSearchActive.value),
              onClick = navigationIconClick(isSearchActive)
            )
          },
          actionMenuItems = actionMenuList(
            isSearchActive = isSearchActive.value,
            onSearchClick = { isSearchActive.value = true },
            onDeleteClick = { pageViewModel.actions.offer(Action.UserClickedDeleteButton) }
          )
        )
      }
    }
  }

  private fun onSwitchCheckedChanged(isChecked: Boolean): () -> Unit = {
    pageSwitchItem.value = pageSwitchItem.value.copy(second = isChecked)
    pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
  }

  private fun navigationIconItem(isSearchActive: Boolean) = if (isSearchActive) {
    IconItem.Drawable(R.drawable.ic_close_white_24dp)
  } else {
    IconItem.Vector(Icons.AutoMirrored.Filled.ArrowBack)
  }

  private fun navigationIconClick(isSearchActive: MutableState<Boolean>): () -> Unit = {
    if (isSearchActive.value) {
      isSearchActive.value = false
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
    pageState.value = state
    pageSwitchItem.value = Triple(switchString, switchIsChecked, !state.isInSelectionState)
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
