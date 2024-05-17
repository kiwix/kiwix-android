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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.databinding.FragmentSearchBinding
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setUpSearchView
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
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchState
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchViewModel
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

const val NAV_ARG_SEARCH_STRING = "searchString"
const val VISIBLE_ITEMS_THRESHOLD = 5
const val LOADING_ITEMS_BEFORE = 3
const val DISABLED_SEARCH_IN_TEXT_OPACITY = 0.6f
const val ENABLED_SEARCH_IN_TEXT_OPACITY = 1f

class SearchFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private var searchView: SearchView? = null
  private var searchInTextMenuItem: MenuItem? = null
  private var findInPageTextView: TextView? = null
  private var fragmentSearchBinding: FragmentSearchBinding? = null

  private val searchViewModel by lazy { viewModel<SearchViewModel>(viewModelFactory) }
  private var searchAdapter: SearchAdapter? = null
  private var isDataLoading = false
  private var renderingJob: Job? = null
  private val searchMutex = Mutex()

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    fragmentSearchBinding = FragmentSearchBinding.inflate(inflater, container, false)
    setupMenu()
    return fragmentSearchBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    searchAdapter = SearchAdapter(
      RecentSearchDelegate(::onItemClick, ::onItemClickNewTab) {
        searchViewModel.actions.trySend(OnItemLongClick(it)).isSuccess
      },
      ZimSearchResultDelegate(::onItemClick, ::onItemClickNewTab)
    )
    setupToolbar(view)
    fragmentSearchBinding?.searchList?.run {
      adapter = searchAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
      // Add scroll listener to detect when the last item is reached
      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(recyclerView, dx, dy)

          val layoutManager = recyclerView.layoutManager as LinearLayoutManager
          val totalItemCount = layoutManager.itemCount
          val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
          // Check if the user is about to reach the last item
          if (!isDataLoading &&
            totalItemCount <= lastVisibleItem + VISIBLE_ITEMS_THRESHOLD - LOADING_ITEMS_BEFORE
          ) {
            // Load more data when the last item is almost visible
            loadMoreSearchResult()
          }
        }
      })
    }
    lifecycleScope.launchWhenCreated {
      searchViewModel.effects.collect { it.invokeWith(this@SearchFragment.coreMainActivity) }
    }
    handleBackPress()
  }

  /**
   * Loads more search results and appends them to the existing search results list in the RecyclerView.
   * This function is typically triggered when the RecyclerView is near about its last item.
   */
  @SuppressLint("CheckResult")
  private fun loadMoreSearchResult() {
    if (isDataLoading) return
    val safeStartIndex = searchAdapter?.itemCount ?: 0
    isDataLoading = true
    // Show a loading indicator while data is being loaded
    fragmentSearchBinding?.loadingMoreDataIndicator?.isShowing(true)
    // Request more search results from the ViewModel, providing the start index and existing results
    searchViewModel.loadMoreSearchResults(safeStartIndex, searchAdapter?.items)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { searchResults ->
        // Hide the loading indicator when data loading is complete
        fragmentSearchBinding?.loadingMoreDataIndicator?.isShowing(false)
        // Update data loading status based on the received search results
        isDataLoading = when {
          searchResults == null -> true
          searchResults.isEmpty() -> false
          else -> {
            // Append the new search results to the existing list
            searchAdapter?.addData(searchResults)
            false
          }
        }
      }
  }

  private fun handleBackPress() {
    activity?.onBackPressedDispatcher?.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          goBack()
        }
      }
    )
  }

  private fun setupToolbar(view: View) {
    view.post {
      with(requireActivity() as CoreMainActivity) {
        setSupportActionBar(view.findViewById(R.id.toolbar))
        supportActionBar?.apply {
          setHomeButtonEnabled(true)
          title = getString(R.string.menu_search_in_text)
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    renderingJob?.cancel()
    renderingJob = null
    activity?.intent?.action = null
    searchView = null
    searchInTextMenuItem = null
    findInPageTextView = null
    searchAdapter = null
    fragmentSearchBinding = null
  }

  private fun goBack() {
    val readerFragmentResId = (activity as CoreMainActivity).readerFragmentResId
    findNavController().popBackStack(readerFragmentResId, false)
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.menu_search, menu)
          val searchMenuItem = menu.findItem(R.id.menu_search)
          searchMenuItem.expandActionView()
          searchView = searchMenuItem.actionView as SearchView
          searchView?.apply {
            setUpSearchView(requireActivity())
            searchView?.setOnQueryTextListener(
              SimpleTextListener { query, isSubmit ->
                if (query.isNotEmpty()) {
                  setIsPageSearchEnabled(true)
                  when {
                    isSubmit -> {
                      // if user press the search/enter button on keyboard,
                      // try to open the article if present
                      getSearchListItemForQuery(query)?.let(::onItemClick)
                    }

                    else -> searchViewModel.actions.trySend(Filter(query)).isSuccess
                  }
                } else {
                  setIsPageSearchEnabled(false)
                }
              }
            )
          }

          searchMenuItem.setOnActionExpandListener(object : OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = false

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
              searchViewModel.actions.trySend(ExitedSearch).isSuccess
              return false
            }
          })
          searchInTextMenuItem = menu.findItem(R.id.menu_searchintext)
          findInPageTextView =
            searchInTextMenuItem?.actionView?.findViewById(R.id.find_in_page_text_view)
          searchInTextMenuItem?.actionView?.setOnClickListener {
            searchViewModel.actions.trySend(ClickedSearchInText).isSuccess
          }
          lifecycleScope.launchWhenCreated {
            searchViewModel.state.collect { render(it) }
          }
          val searchStringFromArguments = arguments?.getString(NAV_ARG_SEARCH_STRING)
          if (searchStringFromArguments != null) {
            searchView?.setQuery(searchStringFromArguments, false)
          }
          searchViewModel.actions.trySend(Action.CreatedWithArguments(arguments)).isSuccess
        }

        override fun onMenuItemSelected(menuItem: MenuItem) = true
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  private fun getSearchListItemForQuery(query: String): SearchListItem? =
    searchAdapter?.items?.firstOrNull {
      it.value.equals(query, ignoreCase = true)
    }

  private suspend fun render(state: SearchState) {
    // Check if the fragment is visible on the screen. This method called multiple times
    // (7-14 times) when an item in the search list is clicked, which leads to unnecessary
    // data loading and also causes a crash.
    // The issue arises because the searchViewModel takes a moment to detach from the window,
    // and during this time, this method is called multiple times due to the rendering process.
    // To avoid unnecessary data loading and prevent crashes, we check if the search screen is
    // visible to the user before proceeding. If the screen is not visible,
    // we skip the data loading process.
    // if (!isVisible) return
    searchMutex.withLock {
      // `cancelAndJoin` cancels the previous running job and waits for it to completely cancel.
      renderingJob?.cancelAndJoin()
      isDataLoading = false
      searchInTextMenuItem?.actionView?.isVisible = state.searchOrigin == FromWebView
      setIsPageSearchEnabled(state.searchTerm.isNotBlank())

      fragmentSearchBinding?.searchLoadingIndicator?.isShowing(true)
      renderingJob = searchViewModel.viewModelScope.launch(Dispatchers.Main) {
        val searchResult = withContext(Dispatchers.IO) {
          state.getVisibleResults(0, renderingJob)
        }

        fragmentSearchBinding?.searchLoadingIndicator?.isShowing(false)

        searchResult?.let {
          fragmentSearchBinding?.searchNoResults?.isVisible = it.isEmpty()
          searchAdapter?.items = it
        }
      }
    }
  }

  private fun setIsPageSearchEnabled(isEnabled: Boolean) {
    searchInTextMenuItem?.actionView?.isEnabled = isEnabled
    findInPageTextView?.alpha = if (isEnabled) {
      ENABLED_SEARCH_IN_TEXT_OPACITY
    } else {
      DISABLED_SEARCH_IN_TEXT_OPACITY
    }
  }

  private fun onItemClick(it: SearchListItem) {
    searchViewModel.actions.trySend(OnItemClick(it)).isSuccess
  }

  private fun onItemClickNewTab(it: SearchListItem) {
    searchViewModel.actions.trySend(OnOpenInNewTabClick(it)).isSuccess
  }

  @Suppress("DEPRECATION")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    searchViewModel.actions.trySend(ActivityResultReceived(requestCode, resultCode, data)).isSuccess
  }
}

private fun ContentLoadingProgressBar.isShowing(show: Boolean) {
  if (show) {
    show()
  } else {
    hide()
  }
}
