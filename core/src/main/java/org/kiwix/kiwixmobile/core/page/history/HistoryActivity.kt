package org.kiwix.kiwixmobile.core.page.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_history.history_switch
import kotlinx.android.synthetic.main.activity_history.no_history
import kotlinx.android.synthetic.main.activity_history.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageActivity
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryAdapter
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryDelegate.HistoryDateDelegate
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryState
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : OnItemClickListener, PageActivity() {
  private var actionMode: ActionMode? = null
  override val pageViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }

  private val historyAdapter: HistoryAdapter by lazy {
    HistoryAdapter(HistoryItemDelegate(this), HistoryDateDelegate())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_history)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(R.string.history)

    recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    recycler_view.adapter = historyAdapter

    compositeDisposable.add(pageViewModel.effects.subscribe { it.invokeWith(this) })
    history_switch.setOnCheckedChangeListener { _, isChecked ->
      pageViewModel.actions.offer(Action.UserClickedShowAllToggle(isChecked))
    }
    history_switch.isChecked = sharedPreferenceUtil.showHistoryAllBooks
  }

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

  override fun render(state: PageState) {
    state as HistoryState
    historyAdapter.items = state.historyListItems
    history_switch.isEnabled = !state.isInSelectionState
    no_history.visibility = if (state.historyListItems.isEmpty()) VISIBLE else GONE
    if (state.isInSelectionState) {
      if (actionMode == null) {
        actionMode = startSupportActionMode(actionModeCallback)
      }
    } else {
      actionMode?.finish()
    }
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

  override fun onItemClick(favicon: ImageView, history: HistoryItem) {
    pageViewModel.actions.offer(OnItemClick(history))
  }

  override fun onItemLongClick(favicon: ImageView, history: HistoryItem): Boolean =
    pageViewModel.actions.offer(OnItemLongClick(history))
}
