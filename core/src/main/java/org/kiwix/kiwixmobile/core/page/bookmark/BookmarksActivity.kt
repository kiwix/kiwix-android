package org.kiwix.kiwixmobile.core.page.bookmark

import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_bookmarks.bookmarks_switch
import kotlinx.android.synthetic.main.activity_bookmarks.no_bookmarks
import kotlinx.android.synthetic.main.activity_bookmarks.recycler_view
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageActivity
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkDelegate.BookmarkItemDelegate
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener

class BookmarksActivity : PageActivity() {
  override val pageViewModel by lazy { viewModel<BookmarkViewModel>(viewModelFactory) }

  override val pageAdapter: PageAdapter by lazy {
    PageAdapter(BookmarkItemDelegate(this))
  }

  override val showAllPagesSwitch: Switch = bookmarks_switch
  override val noItems: TextView = no_bookmarks
  override val recyclerView: RecyclerView = recycler_view
  override val layoutId: Int = R.layout.activity_bookmarks
  override val title: String = getString(R.string.bookmarks)
  override val switchIsChecked: Boolean = sharedPreferenceUtil.showBookmarksAllBooks

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_bookmarks, menu)
    val search = menu.findItem(R.id.menu_bookmarks_search).actionView as SearchView
    search.queryHint = getString(R.string.search_bookmarks)
    search.setOnQueryTextListener(SimpleTextListener {
      pageViewModel.actions.offer(Filter(it))
    })
    pageViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      pageViewModel.actions.offer(Action.Exit)
    }
    if (item.itemId == R.id.menu_bookmarks_clear) {
      pageViewModel.actions.offer(Action.UserClickedDeleteButton)
    }
    return super.onOptionsItemSelected(item)
  }
}
