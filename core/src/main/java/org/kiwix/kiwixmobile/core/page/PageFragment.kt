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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_page.no_page
import kotlinx.android.synthetic.main.fragment_page.page_switch
import kotlinx.android.synthetic.main.fragment_page.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

abstract class PageFragment : OnItemClickListener, BaseFragment(), BaseFragmentActivityExtensions {
  abstract val pageViewModel: PageViewModel<*, *>
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private var actionMode: ActionMode? = null
  val compositeDisposable = CompositeDisposable()
  abstract val title: String
  abstract val noItemsString: String
  abstract val switchString: String
  abstract val searchQueryHint: String
  abstract val pageAdapter: PageAdapter
  abstract val switchIsChecked: Boolean

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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_page, menu)
    val search = menu.findItem(R.id.menu_page_search).actionView as SearchView
    search.queryHint = searchQueryHint
    search.setOnQueryTextListener(SimpleTextListener {
      pageViewModel.actions.offer(Action.Filter(it))
    })
    pageViewModel.state.observe(this, Observer(::render))
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      pageViewModel.actions.offer(Action.Exit)
    }
    if (item.itemId == R.id.menu_pages_clear) {
      pageViewModel.actions.offer(Action.UserClickedDeleteButton)
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    val activity = requireActivity() as CoreMainActivity
    activity.setSupportActionBar(toolbar)

    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title = title
    }
    recycler_view.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    recycler_view.adapter = pageAdapter

    no_page.text = noItemsString

    page_switch.text = switchString
    page_switch.isChecked = switchIsChecked
    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(activity) })
    page_switch.setOnCheckedChangeListener { _, isChecked ->
      pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    setHasOptionsMenu(true)
    return inflater.inflate(R.layout.fragment_page, container, false)
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  private fun render(state: PageState<*>) {
    pageAdapter.items = state.visiblePageItems
    page_switch.isEnabled = !state.isInSelectionState
    no_page.visibility = if (state.pageItems.isEmpty()) VISIBLE else GONE
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
