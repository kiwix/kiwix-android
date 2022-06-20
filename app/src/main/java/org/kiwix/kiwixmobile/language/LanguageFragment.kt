/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_language.language_progressbar
import kotlinx.android.synthetic.main.activity_language.language_recycler_view
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.SimpleTextListener
import org.kiwix.kiwixmobile.language.adapter.LanguageAdapter
import org.kiwix.kiwixmobile.language.adapter.LanguageDelegate.HeaderDelegate
import org.kiwix.kiwixmobile.language.adapter.LanguageDelegate.LanguageItemDelegate
import org.kiwix.kiwixmobile.language.viewmodel.Action
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.LanguageViewModel
import org.kiwix.kiwixmobile.language.viewmodel.State
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import javax.inject.Inject

class LanguageFragment : BaseFragment() {

  private val languageViewModel by lazy { viewModel<LanguageViewModel>(viewModelFactory) }

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private val compositeDisposable = CompositeDisposable()

  private val languageAdapter =
    LanguageAdapter(
      LanguageItemDelegate { languageViewModel.actions.offer(Select(it)) },
      HeaderDelegate()
    )

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val activity = requireActivity() as CoreMainActivity
    activity.setSupportActionBar(view.findViewById(R.id.toolbar))

    activity.supportActionBar?.let {
      it.setDisplayHomeAsUpEnabled(true)
      it.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp)
      it.setTitle(R.string.select_languages)
    }
    language_recycler_view.run {
      adapter = languageAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    languageViewModel.state.observe(viewLifecycleOwner, Observer(::render))
    compositeDisposable.add(
      languageViewModel.effects.subscribe(
        {
          it.invokeWith(activity)
        },
        Throwable::printStackTrace
      )
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    setHasOptionsMenu(true)
    return inflater.inflate(R.layout.activity_language, container, false)
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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_language, menu)
    val search = menu.findItem(R.id.menu_language_search)
    (search.actionView as SearchView).setOnQueryTextListener(
      SimpleTextListener {
        languageViewModel.actions.offer(Filter(it))
      }
    )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_language_save -> {
        languageViewModel.actions.offer(Action.SaveAll)
        closeKeyboard()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
