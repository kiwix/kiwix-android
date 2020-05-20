package org.kiwix.kiwixmobile.core.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_history.history_switch
import kotlinx.android.synthetic.main.activity_history.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.id
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.adapter.HistoryAdapter2
import org.kiwix.kiwixmobile.core.history.adapter.HistoryDelegate.HistoryDateDelegate
import org.kiwix.kiwixmobile.core.history.adapter.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.RequestDeleteAllHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.RequestDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ToggleShowHistoryFromAllBooks
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.history.viewmodel.State
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.utils.DialogShower
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : OnItemClickListener, BaseActivity() {
  private val activityComponent by lazy { coreActivityComponent }
  private var actionMode: ActionMode? = null
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val historyViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()

  @Inject lateinit var dialogShower: DialogShower

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
        if (item.itemId == id.menu_context_delete) {
          historyViewModel.actions.offer(RequestDeleteSelectedHistoryItems(dialogShower))
          return true
        }
        historyViewModel.actions.offer(Action.ExitActionModeMenu)
        return false
      }

      override fun onDestroyActionMode(mode: ActionMode) {
        historyViewModel.actions.offer(Action.ExitActionModeMenu)
        actionMode = null
      }
    }

  private val historyAdapter: HistoryAdapter2 by lazy {
    HistoryAdapter2(HistoryItemDelegate(this), HistoryDateDelegate())
  }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_history)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTitle(string.history)

    recycler_view.adapter = historyAdapter
    recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

    history_switch.setOnCheckedChangeListener { _, isChecked ->
      historyViewModel.actions.offer(ToggleShowHistoryFromAllBooks(isChecked))
    }
    history_switch.isChecked = !sharedPreferenceUtil.showHistoryCurrentBook

    compositeDisposable.add(historyViewModel.effects.subscribe { it.invokeWith(this) })
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_history, menu)
    val search = menu.findItem(id.menu_history_search).actionView as SearchView
    search.queryHint = getString(string.search_history)
    search.setOnQueryTextListener(SimpleTextListener {
      historyViewModel.actions.offer(Filter(it))
    })
    historyViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      historyViewModel.actions.offer(ExitHistory)
      return true
    }
    if (item.itemId == R.id.menu_history_clear) {
      historyViewModel.actions.offer(RequestDeleteAllHistoryItems(dialogShower))
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun render(state: State) =
    when (state) {
      is Results -> {
        actionMode?.finish()
        historyAdapter.items = state.historyItems
        history_switch.isEnabled = true
        render(state.searchString)
      }
      is SelectionResults -> {
        if (state.selectedHistoryItems.isNotEmpty() && actionMode == null) {
          actionMode = startSupportActionMode(actionModeCallback)
        }
        historyAdapter.items = state.historyItems
        history_switch.isEnabled = false
      }
      is NoResults -> {
      }
    }

  private fun render(searchString: String) {
  }

  override fun onItemClick(
    favicon: ImageView,
    history: HistoryItem
  ) {
    historyViewModel.actions.offer(Action.OnItemClick(history))
  }

  override fun onItemLongClick(
    favicon: ImageView,
    history: HistoryItem
  ): Boolean {
    return historyViewModel.actions.offer(OnItemLongClick(history))
  }
}
