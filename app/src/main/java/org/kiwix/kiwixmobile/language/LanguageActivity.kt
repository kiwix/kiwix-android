package org.kiwix.kiwixmobile.language

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_language.language_progressbar
import kotlinx.android.synthetic.main.activity_language.language_recycler_view
import kotlinx.android.synthetic.main.activity_language.toolbar
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.base.BaseActivity
import org.kiwix.kiwixmobile.extensions.viewModel
import org.kiwix.kiwixmobile.language.adapter.LanguageAdapter
import org.kiwix.kiwixmobile.language.adapter.LanguageDelegate.HeaderDelegate
import org.kiwix.kiwixmobile.language.adapter.LanguageDelegate.LanguageItemDelegate
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import org.kiwix.kiwixmobile.zim_manager.SimpleTextListener
import javax.inject.Inject

class LanguageActivity : BaseActivity() {

  private val languageViewModel by lazy { viewModel<LanguageViewModel>(viewModelFactory) }

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private val compositeDisposable = CompositeDisposable()

  private val languageAdapter =
    LanguageAdapter(
      LanguageItemDelegate { languageViewModel.actions.offer(Select(it)) },
      HeaderDelegate()
    )

  override fun injection() {
    KiwixApplication.getApplicationComponent().inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_language)
    setSupportActionBar(toolbar)

    supportActionBar?.let {
      it.setDisplayHomeAsUpEnabled(true)
      it.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp)
      it.setTitle(R.string.select_languages)
    }
    language_recycler_view.run {
      adapter = languageAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    languageViewModel.state.observe(this, Observer(::render))
    compositeDisposable.add(
      languageViewModel.effects.subscribe(
        {
          it.invokeWith(this)
        },
        Throwable::printStackTrace
      )
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  private fun render(state: State) = when (state) {
    Loading -> language_progressbar.show()
    is Content -> {
      language_progressbar.hide()
      languageAdapter.items = state.viewItems
    }
    Saving -> Unit
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_language, menu)
    val search = menu.findItem(R.id.menu_language_search)
    (search.actionView as SearchView).setOnQueryTextListener(SimpleTextListener {
      languageViewModel.actions.offer(Action.Filter(it))
    })
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        onBackPressed()
        return true
      }
      R.id.menu_language_save -> {
        languageViewModel.actions.offer(Action.SaveAll)
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }
}
