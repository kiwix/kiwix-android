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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

abstract class PageFragment : OnItemClickListener, BaseFragment(), FragmentActivityExtensions {
  abstract val pageViewModel: PageViewModel<*, *>
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private var actionMode: ActionMode? = null
  val compositeDisposable = CompositeDisposable()
  abstract val screenTitle: String
  abstract val noItemsString: String
  abstract val switchString: String
  abstract val searchQueryHint: String
  abstract val pageAdapter: PageAdapter
  abstract val switchIsChecked: Boolean
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.menu_page, menu)
          val search = menu.findItem(R.id.menu_page_search).actionView as SearchView
          search.queryHint = searchQueryHint
          search.setOnQueryTextListener(
            SimpleTextListener {
              pageViewModel.actions.offer(Action.Filter(it))
            }
          )
        }

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
    setupMenu()
    val activity = requireActivity() as CoreMainActivity
    fragmentPageBinding?.recyclerView?.layoutManager =
      LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    fragmentPageBinding?.recyclerView?.adapter = pageAdapter
    val toolbar = fragmentPageBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
    toolbar?.apply {
      activity.setSupportActionBar(this)
      setNavigationOnClickListener { requireActivity().onBackPressed() }
    }
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title = screenTitle
    }
    fragmentPageBinding?.noPage?.text = noItemsString

    fragmentPageBinding?.pageSwitch?.text = switchString
    fragmentPageBinding?.pageSwitch?.isChecked = switchIsChecked
    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(activity) })
    fragmentPageBinding?.pageSwitch?.setOnCheckedChangeListener { _, isChecked ->
      pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    }
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
    fragmentPageBinding = FragmentPageBinding.inflate(inflater, container, false)
    return fragmentPageBinding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
    fragmentPageBinding?.recyclerView?.adapter = null
    fragmentPageBinding = null
  }

  private fun render(state: PageState<*>) {
    pageAdapter.items = state.visiblePageItems
    fragmentPageBinding?.pageSwitch?.isEnabled = !state.isInSelectionState
    fragmentPageBinding?.noPage?.visibility = if (state.pageItems.isEmpty()) VISIBLE else GONE
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
