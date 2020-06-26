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
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.page.adapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import javax.inject.Inject

abstract class PageActivity : OnItemClickListener, BaseActivity() {
  val activityComponent by lazy { coreActivityComponent }
  abstract val pageViewModel: PageViewModel
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private var actionMode: ActionMode? = null
  val compositeDisposable = CompositeDisposable()
  abstract val pageAdapter: PageAdapter
  abstract val showAllPagesSwitch: Switch
  abstract val noItems: TextView
  abstract val recyclerView: RecyclerView
  abstract val layoutId: Int
  abstract val title: String
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layoutId)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.title = title
    recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    recyclerView.adapter = pageAdapter

    showAllPagesSwitch.isChecked = switchIsChecked
    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(this) })
    showAllPagesSwitch.setOnCheckedChangeListener { _, isChecked ->
      pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    }
  }

  fun render(state: PageState) {
    pageAdapter.items = state.filteredPageItems
    showAllPagesSwitch.isEnabled = !state.isInSelectionState
    noItems.visibility = if (state.pageItems.isEmpty()) VISIBLE else GONE
    if (state.isInSelectionState) {
      if (actionMode == null) {
        actionMode = startSupportActionMode(actionModeCallback)
      }
      actionMode?.title = getString(R.string.selected_items, state.numberOfSelectedItems)
    } else {
      actionMode?.finish()
    }
  }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  override fun onItemClick(page: Page) {
    pageViewModel.actions.offer(Action.OnItemClick(page))
  }

  override fun onItemLongClick(page: Page): Boolean =
    pageViewModel.actions.offer(Action.OnItemLongClick(page))
}
