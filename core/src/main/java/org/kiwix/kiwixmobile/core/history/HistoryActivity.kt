package org.kiwix.kiwixmobile.core.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
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
import kotlinx.android.synthetic.main.activity_history.no_history
import kotlinx.android.synthetic.main.activity_history.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.history.adapter.HistoryAdapter
import org.kiwix.kiwixmobile.core.history.adapter.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.adapter.HistoryDelegate.HistoryDateDelegate
import org.kiwix.kiwixmobile.core.history.adapter.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ExitHistory
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedDeleteSelectedHistoryItems
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryState
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import javax.inject.Inject

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : OnItemClickListener, BaseActivity() {
  val activityComponent by lazy { coreActivityComponent }
  private var actionMode: ActionMode? = null
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val historyViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()

  private val actionModeCallback: Callback =
    object : Callback {
      override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_context_delete, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

      override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_context_delete) {
          historyViewModel.actions.offer(UserClickedDeleteSelectedHistoryItems)
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

  private val historyAdapter: HistoryAdapter by lazy {
    HistoryAdapter(HistoryItemDelegate(this), HistoryDateDelegate())
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
      historyViewModel.actions.offer(UserClickedShowAllToggle(isChecked))
    }
    history_switch.isChecked = sharedPreferenceUtil.showHistoryAllBooks

    compositeDisposable.add(historyViewModel.effects.subscribe { it.invokeWith(this) })
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_history, menu)
    val search = menu.findItem(R.id.menu_history_search).actionView as SearchView
    search.queryHint = getString(R.string.search_history)
    search.setOnQueryTextListener(SimpleTextListener {
      historyViewModel.actions.offer(Filter(it))
    })
    historyViewModel.state.observe(this, Observer(::render))
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      historyViewModel.actions.offer(ExitHistory)
    }
    if (item.itemId == R.id.menu_history_clear) {
      historyViewModel.actions.offer(UserClickedDeleteButton)
    }
    return super.onOptionsItemSelected(item)
  }

  private fun render(state: HistoryState) {
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

  override fun onItemClick(favicon: ImageView, history: HistoryItem) {
    historyViewModel.actions.offer(Action.OnItemClick(history))
  }

  override fun onItemLongClick(favicon: ImageView, history: HistoryItem): Boolean =
    historyViewModel.actions.offer(OnItemLongClick(history))
}
