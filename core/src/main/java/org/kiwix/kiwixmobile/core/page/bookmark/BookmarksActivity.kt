package org.kiwix.kiwixmobile.core.page.bookmark

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_bookmarks.bookmarks_switch
import kotlinx.android.synthetic.main.activity_bookmarks.no_bookmarks
import kotlinx.android.synthetic.main.activity_bookmarks.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkDelegate.BookmarkItemDelegate
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarksAdapter
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarksAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkState
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

class BookmarksActivity : OnItemClickListener, BaseActivity() {
  val activityComponent by lazy { coreActivityComponent }
  private var actionMode: ActionMode? = null
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  val compositeDisposable = CompositeDisposable()
  private val bookmarkViewModel by lazy { viewModel<BookmarkViewModel>(viewModelFactory) }

  private val actionModeCallback: ActionMode.Callback =
    object : ActionMode.Callback {
      override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_context_delete, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

      override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_context_delete) {
          bookmarkViewModel.actions.offer(Action.UserClickedDeleteSelectedPages)
          return true
        }
        bookmarkViewModel.actions.offer(Action.ExitActionModeMenu)
        return false
      }

      override fun onDestroyActionMode(mode: ActionMode) {
        bookmarkViewModel.actions.offer(Action.ExitActionModeMenu)
        actionMode = null
      }
    }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  private val bookmarksAdapter: BookmarksAdapter by lazy {
    BookmarksAdapter(BookmarkItemDelegate(this))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmarks)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(R.string.bookmarks)

    recycler_view.adapter = bookmarksAdapter
    recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    bookmarks_switch.setOnCheckedChangeListener { _, isChecked ->
      bookmarkViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    }
    bookmarks_switch.isChecked = sharedPreferenceUtil.showBookmarksAllBooks

    compositeDisposable.add(bookmarkViewModel.effects.subscribe { it.invokeWith(this) })
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_bookmarks, menu)
    val search = menu.findItem(R.id.menu_bookmarks_search).actionView as SearchView
    search.queryHint = getString(R.string.search_bookmarks)
    search.setOnQueryTextListener(SimpleTextListener {
      bookmarkViewModel.actions.offer(Filter(it))
    })
    bookmarkViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      bookmarkViewModel.actions.offer(Action.Exit)
    }
    if (item.itemId == R.id.menu_bookmarks_clear) {
      bookmarkViewModel.actions.offer(Action.UserClickedDeleteButton)
    }
    return super.onOptionsItemSelected(item)
  }

  fun render(state: BookmarkState) {
    val filteredBookmarks = state.filteredBookmarks
    filteredBookmarks.let { bookmarksAdapter.items = it }
    toggleNoBookmarksText(filteredBookmarks)
    bookmarks_switch.isEnabled = !state.isInSelectionState
    if (state.isInSelectionState) {
      handleSelectionState(filteredBookmarks)
    } else {
      actionMode?.finish()
    }
  }

  private fun handleSelectionState(filteredBookmarks: List<BookmarkItem>) {
    if (actionMode == null) {
      actionMode = startSupportActionMode(actionModeCallback)
    }
    val numberOfSelectedItems = filteredBookmarks.filter(BookmarkItem::isSelected).size
    actionMode?.title = getString(R.string.selected_items, numberOfSelectedItems)
  }

  private fun toggleNoBookmarksText(items: List<BookmarkItem>) {
    if (items.isEmpty()) {
      no_bookmarks.visibility = View.VISIBLE
    } else {
      no_bookmarks.visibility = View.GONE
    }
  }

  override fun onItemClick(bookmark: BookmarkItem) {
    bookmarkViewModel.actions.offer(OnItemClick(bookmark))
  }

  override fun onItemLongClick(bookmark: BookmarkItem): Boolean =
    bookmarkViewModel.actions.offer(OnItemLongClick(bookmark))
}
