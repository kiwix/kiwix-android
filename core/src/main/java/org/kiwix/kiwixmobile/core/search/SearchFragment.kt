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
package org.kiwix.kiwixmobile.core.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_search.searchViewAnimator
import kotlinx.android.synthetic.main.activity_search.search_list
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.setDistinctDisplayedChild
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.adapter.SearchAdapter
import org.kiwix.kiwixmobile.core.search.adapter.SearchDelegate.RecentSearchDelegate
import org.kiwix.kiwixmobile.core.search.adapter.SearchDelegate.ZimSearchResultDelegate
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ActivityResultReceived
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ClickedSearchInText
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.ExitedSearch
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnOpenInNewTabClick
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchOrigin.FromWebView
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.search.viewmodel.State
import org.kiwix.kiwixmobile.core.search.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.search.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

const val NAV_ARG_SEARCH_STRING = "searchString"

class SearchFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private lateinit var searchView: SearchView
  private lateinit var searchInTextMenuItem: MenuItem

  private val searchViewModel by lazy { viewModel<SearchViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()
  private val searchAdapter: SearchAdapter by lazy {
    SearchAdapter(
      RecentSearchDelegate(::onItemClick, ::onItemClickNewTab) {
        searchViewModel.actions.offer(OnItemLongClick(it))
      },
      ZimSearchResultDelegate(::onItemClick, ::onItemClickNewTab)
    )
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.activity_search, container, false)
    setHasOptionsMenu(true)
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar(view)
    search_list.run {
      adapter = searchAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    compositeDisposable.add(searchViewModel.effects.subscribe {
      it.invokeWith(requireActivity() as CoreMainActivity)
    })
  }

  private fun setupToolbar(view: View) {
    with(requireActivity() as CoreMainActivity) {
      setSupportActionBar(view.findViewById(R.id.toolbar))
      supportActionBar?.apply {
        setHomeButtonEnabled(true)
        title = getString(R.string.menu_search_in_text)
      }
    }
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_search, menu)
    val searchMenuItem = menu.findItem(R.id.menu_search)
    searchMenuItem.expandActionView()
    searchView = searchMenuItem.actionView as SearchView
    searchView.setOnQueryTextListener(SimpleTextListener {
      searchViewModel.actions.offer(Filter(it))
    })
    searchMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem) = false

      override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        searchViewModel.actions.offer(ExitedSearch)
        return false
      }
    })
    searchInTextMenuItem = menu.findItem(R.id.menu_searchintext)
    searchInTextMenuItem.setOnMenuItemClickListener {
      searchViewModel.actions.offer(ClickedSearchInText)
      true
    }

    searchViewModel.state.observe(this, Observer(::render))
    searchViewModel.actions.offer(Action.CreatedWithArguments(arguments))
  }

  private fun render(state: State) {
    searchInTextMenuItem.isVisible = state.searchOrigin == FromWebView
    when (state) {
      is Results -> {
        searchViewAnimator.setDistinctDisplayedChild(0)
        searchAdapter.items = state.values
        render(state.searchString)
      }
      is NoResults -> {
        searchViewAnimator.setDistinctDisplayedChild(1)
        render(state.searchString)
      }
    }
  }

  private fun render(searchString: String) {
    searchInTextMenuItem.isEnabled = searchString.isNotBlank()
  }

  private fun onItemClick(it: SearchListItem) {
    searchViewModel.actions.offer(OnItemClick(it))
  }

  private fun onItemClickNewTab(it: SearchListItem) {
    searchViewModel.actions.offer(OnOpenInNewTabClick(it))
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    searchViewModel.actions.offer(ActivityResultReceived(requestCode, resultCode, data))
  }
}
