package org.kiwix.kiwixmobile.core.bookmark

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_bookmarks.bookmarks_switch
import kotlinx.android.synthetic.main.activity_bookmarks.no_bookmarks
import kotlinx.android.synthetic.main.activity_history.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkDelegate.BookmarkItemDelegate
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarksAdapter
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarksAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.RequestDeleteAllBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.RequestDeleteSelectedBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ToggleShowBookmarksFromAllBooks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

class BookmarksActivity : OnItemClickListener, BaseActivity() {
  val activityComponent by lazy { coreActivityComponent }
  private var actionMode: ActionMode? = null
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val bookmarksViewModel by lazy { viewModel<BookmarkViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()

  private val actionModeCallback: Callback =
    object : Callback {
      override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_context_delete, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
      }

      override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_context_delete) {
          bookmarksViewModel.actions.offer(RequestDeleteSelectedBookmarks)
          return true
        }
        bookmarksViewModel.actions.offer(Action.ExitActionModeMenu)
        return false
      }

      override fun onDestroyActionMode(mode: ActionMode) {
        bookmarksViewModel.actions.offer(Action.ExitActionModeMenu)
        actionMode = null
      }
    }

  private val bookmarksAdapter: BookmarksAdapter by lazy {
    BookmarksAdapter(BookmarkItemDelegate(this))
  }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmarks)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(string.bookmarks)

    recycler_view.adapter = bookmarksAdapter
    recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    bookmarks_switch.setOnCheckedChangeListener { _, isChecked ->
      bookmarksViewModel.actions.offer(ToggleShowBookmarksFromAllBooks(isChecked))
    }
    bookmarks_switch.isChecked = sharedPreferenceUtil.showBookmarksAllBooks

    compositeDisposable.add(bookmarksViewModel.effects.subscribe { it.invokeWith(this) })
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_bookmarks, menu)
    val search = menu.findItem(R.id.menu_bookmarks_search).actionView as SearchView
    search.queryHint = getString(R.string.search_bookmarks)
    search.setOnQueryTextListener(SimpleTextListener {
      bookmarksViewModel.actions.offer(Filter(it))
    })
    bookmarksViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      bookmarksViewModel.actions.offer(ExitBookmarks)
    }
    if (item.itemId == R.id.menu_bookmarks_clear) {
      bookmarksViewModel.actions.offer(RequestDeleteAllBookmarks)
    }
    return super.onOptionsItemSelected(item)
  }

  private fun render(state: State) =
    when (state) {
      is Results -> {
        actionMode?.finish()
        state.bookmarkItems?.let { bookmarksAdapter.items = it }
        bookmarks_switch.isEnabled = true
        no_bookmarks.visibility = View.GONE
      }
      is SelectionResults -> {
        if (state.bookmarkItems?.any { it.isSelected } == true &&
          actionMode == null) {
          actionMode = startSupportActionMode(actionModeCallback)
        }
        val numberOfSelectedItems = state.bookmarkItems?.filter { it.isSelected }?.size
        actionMode?.setTitle(getString(R.string.selected_items, numberOfSelectedItems))
        state.bookmarkItems?.let { bookmarksAdapter.items = it }
        bookmarks_switch.isEnabled = false
        no_bookmarks.visibility = View.GONE
      }
      is NoResults -> {
        state.bookmarkItems?.let { bookmarksAdapter.items = it }
        no_bookmarks.visibility = View.VISIBLE
      }
    }

  override fun onItemClick(bookmark: BookmarkItem) {
    bookmarksViewModel.actions.offer(Action.OnItemClick(bookmark))
  }

  override fun onItemLongClick(bookmark: BookmarkItem): Boolean =
    bookmarksViewModel.actions.offer(OnItemLongClick(bookmark))
}
