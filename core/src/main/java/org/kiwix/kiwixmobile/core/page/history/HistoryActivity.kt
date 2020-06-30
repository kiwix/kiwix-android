package org.kiwix.kiwixmobile.core.page.history

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageActivity
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryDelegate.HistoryDateDelegate
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : PageActivity() {
  override val pageViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }

  override val pageAdapter by lazy {
    PageAdapter(HistoryItemDelegate(this), HistoryDateDelegate())
  }

  override val noItemsString: String by lazy { getString(R.string.no_history) }
  override val switchString: String by lazy { getString(R.string.history_from_current_book) }
  override val title: String by lazy { getString(R.string.history) }
  override val switchIsChecked: Boolean by lazy { sharedPreferenceUtil.showHistoryAllBooks }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_history, menu)
    val search = menu.findItem(R.id.menu_history_search).actionView as SearchView
    search.queryHint = getString(R.string.search_history)
    search.setOnQueryTextListener(SimpleTextListener {
      pageViewModel.actions.offer(Filter(it))
    })
    pageViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      pageViewModel.actions.offer(Action.Exit)
    }
    if (item.itemId == R.id.menu_history_clear) {
      pageViewModel.actions.offer(Action.UserClickedDeleteButton)
    }
    return super.onOptionsItemSelected(item)
  }
}
