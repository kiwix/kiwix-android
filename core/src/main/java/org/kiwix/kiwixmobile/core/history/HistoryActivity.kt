package org.kiwix.kiwixmobile.core.history

import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.Switch
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
import org.kiwix.kiwixmobile.core.R2.id.menu_history_search
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.adapter.HistoryAdapter2
import org.kiwix.kiwixmobile.core.history.adapter.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.Action
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.OnSwitch
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryViewModel
import org.kiwix.kiwixmobile.core.history.viewmodel.State
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import java.util.ArrayList
import javax.inject.Inject

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : OnItemClickListener, BaseActivity() {
  private val activityComponent by lazy { coreActivityComponent }
  private val historyList: List<HistoryListItem> = ArrayList()
  private val fullHistory: List<HistoryListItem> = ArrayList()
  private val search = menu_history_search
  private val deleteList: List<HistoryListItem> = ArrayList()
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private lateinit var recyclerView: RecyclerView
  private val historyViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()

  private val historyAdapter: HistoryAdapter2 by lazy {
    HistoryAdapter2(
      HistoryItemDelegate(deleteList, this)
    )
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

    history_switch.setOnCheckedChangeListener { button, isChecked ->
      historyViewModel.actions.offer(OnSwitch(isChecked))
    }
    history_switch.isChecked = !sharedPreferenceUtil.showHistoryCurrentBook

    compositeDisposable.add(historyViewModel.effects.subscribe{it.invokeWith(this)})
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

  private fun render(state: State) {
    when (state) {
      is Results -> {
        historyAdapter.items = state.historyItems
        render(state.searchString)
      }
      is NoResults -> {
        render(state.searchString)
      }
      is SelectionResults -> {
        historyAdapter.items = state.historyItems
        render(state.searchString)
      }
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
  ): Boolean =
    historyViewModel.actions.offer(OnItemLongClick(history))

}
