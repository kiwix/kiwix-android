package org.kiwix.kiwixmobile.core.history

import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_history.recycler_view
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.id
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.history.HistoryAdapter.OnItemClickListener
import org.kiwix.kiwixmobile.core.history.HistoryDelegate.HistoryItemDelegate
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.ViewModel.HistoryViewModel
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.search.viewmodel.State
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import java.util.ArrayList
import javax.inject.Inject

class HistoryActivity : OnItemClickListener, BaseActivity(){

  val activityComponent by lazy { coreActivityComponent }
  private val historyList: List<HistoryListItem> = ArrayList()
  private val fullHistory: List<HistoryListItem> = ArrayList()
  private val deleteList: List<HistoryListItem> = ArrayList()
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val recyclerView: RecyclerView = recycler_view
  private val historyViewModel by lazy { viewMdel<HistoryViewModel>(viewModelFactory) }
  private val compositeDisposable = CompositeDisposable()
  private val historyAdapter: HistoryAdapter2 by lazy {
    HistoryAdapter2(
      HistoryItemDelegate(deleteList,this){
        historyViewModel.actions.offer(OnItemLongClick(it))
      }
    )
  }

  override fun injection(coreComponent: CoreComponent) {
    coreComponent.activityComponentBuilder()
      .activity(this)
      .build()
      .inject(this)
    activityComponent.inject(this);
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_history)
    setSupportActionBar(toolbar)
    val actionBar = supportActionBar
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true)
      actionBar.setTitle(string.history)
    }
    recyclerView.run{
      adapter = historyAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false);
      setHasFixedSize(true)
    }
    compositeDisposable.add(historyViewModel.effects.subcribe{ it.invokeWith(this) })
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_history, menu)
    val search = menu.findItem(id.menu_history_search).actionView as SearchView
    search.queryHint = getString(string.search_history)
    search.setOnQueryTextListener(SimpleTextListener{historyViewModel.actions.offer(Filter(it))})
    return true
  }


  private fun render(state: State) {

  }

  private fun onItemClick(it: SearchListItem) {
    searchViewModel.actions.offer(OnItemClick(it))
  }

  override fun onItemClick(
    favicon: ImageView,
    history: HistoryItem
  ) {
    TODO("Not yet implemented")
  }

  override fun onItemLongClick(
    favicon: ImageView,
    history: HistoryItem
  ): Boolean {
    TODO("Not yet implemented")
  }

}
