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
package org.kiwix.kiwixmobile.zim_manager

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.System
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.zim_manager.manageViewPager
import kotlinx.android.synthetic.main.zim_manager.tabs
import kotlinx.android.synthetic.main.zim_manager.toolbar
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.startWithActionFrom
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.language.LanguageActivity
import org.kiwix.kiwixmobile.local_file_transfer.LocalFileTransferActivity
import javax.inject.Inject

class ZimManageActivity : BaseActivity() {

  val cachedComponent by lazy { kiwixActivityComponent }

  private val zimManageViewModel by lazy { viewModel<ZimManageViewModel>(viewModelFactory) }
  private val mSectionsPagerAdapter: SectionsPagerAdapter by lazy {
    SectionsPagerAdapter(this, supportFragmentManager)
  }

  private var searchItem: MenuItem? = null
  private var languageItem: MenuItem? = null
  private var getZimItem: MenuItem? = null

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var languagesDao: NewLanguagesDao

  override fun injection() {
    cachedComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LanguageUtils.handleLocaleChange(this, sharedPreferenceUtil)
    setContentView(R.layout.zim_manager)

    setUpToolbar()
    manageViewPager.run {
      adapter = mSectionsPagerAdapter
      offscreenPageLimit = 2
      tabs.setupWithViewPager(this)
      addOnPageChangeListener(SimplePageChangeListener(::updateMenu))
    }
    setViewPagerPositionFromIntent(intent)
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setViewPagerPositionFromIntent(intent)
  }

  private fun setViewPagerPositionFromIntent(intent: Intent?) {
    if (intent?.hasExtra(TAB_EXTRA) == true) {
      manageViewPager.currentItem = intent.getIntExtra(TAB_EXTRA, 0)
    }
  }

  private fun updateMenu(position: Int) {
    searchItem?.isVisible = position == 1
    languageItem?.isVisible = position == 1
    getZimItem?.isVisible = position == 0
  }

  private fun setUpToolbar() {
    setSupportActionBar(toolbar)
    supportActionBar!!.setHomeButtonEnabled(true)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    supportActionBar!!.setTitle(R.string.zim_manager)
    toolbar.setNavigationOnClickListener { onBackPressed() }
    toolbar.setOnClickListener {
      if (manageViewPager.currentItem == 1)
        searchItem?.expandActionView()
    }
  }

  override fun onBackPressed() {
    val value = System.getInt(contentResolver, System.ALWAYS_FINISH_ACTIVITIES, 0)
    if (value == 1) {
      startWithActionFrom<CoreMainActivity>()
    } else {
      super.onBackPressed() // optional depending on your needs
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_zim_manager, menu)
    searchItem = menu.findItem(R.id.action_search)
    languageItem = menu.findItem(R.id.select_language)
    getZimItem = menu.findItem(R.id.get_zim_nearby_device)
    val searchView = searchItem!!.actionView as SearchView
    updateMenu(manageViewPager.currentItem)
    searchView.setOnQueryTextListener(
      SimpleTextListener(zimManageViewModel.requestFiltering::onNext)
    )
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.select_language -> start<LanguageActivity>()
      R.id.get_zim_nearby_device -> start<LocalFileTransferActivity>()
    }
    return super.onOptionsItemSelected(item)
  }

  companion object {
    const val TAB_EXTRA = "TAB"
  }
}
