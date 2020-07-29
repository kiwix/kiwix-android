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
package org.kiwix.kiwixmobile.core.main

import android.content.Intent
import android.view.ActionMode
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.navigation.NavigationView
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.intent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.help.HelpActivity
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksActivity
import org.kiwix.kiwixmobile.core.page.history.HistoryActivity
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.BOOKMARK_CHOSEN_REQUEST
import org.kiwix.kiwixmobile.core.utils.KIWIX_SUPPORT_WEBSITE
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.REQUEST_HISTORY_ITEM_CHOSEN
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

abstract class CoreMainActivity : BaseActivity(), WebViewProvider,
  NavigationView.OnNavigationItemSelectedListener {

  @Inject lateinit var alertDialogShower: AlertDialogShower
  protected lateinit var drawerToggle: ActionBarDrawerToggle

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    supportFragmentManager.fragments.forEach { it.onActivityResult(requestCode, resultCode, data) }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    supportFragmentManager.fragments.forEach {
      it.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override fun onActionModeStarted(mode: ActionMode) {
    super.onActionModeStarted(mode)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onActionModeStarted(mode, this)
    }
  }

  override fun onActionModeFinished(mode: ActionMode) {
    super.onActionModeFinished(mode)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onActionModeFinished(mode, this)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      it.onNewIntent(intent, this)
    }
  }

  override fun getCurrentWebView(): KiwixWebView? {
    return supportFragmentManager.fragments.filterIsInstance<WebViewProvider>().firstOrNull()
      ?.getCurrentWebView()
  }

  abstract fun setupDrawerToggle(toolbar: Toolbar)

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_support_kiwix -> openSupportKiwixExternalLink()
      R.id.menu_settings -> openSettingsActivity()
      R.id.menu_help -> start<HelpActivity>()
      R.id.menu_history -> openHistoryActivity()
      R.id.menu_bookmarks_list -> openBookmarksActivity()
      else -> return false
    }
    return true
  }

  override fun onBackPressed() {
    if (navigationDrawerIsOpen()) {
      closeNavigationDrawer()
      return
    }
    supportFragmentManager.fragments.filterIsInstance<BaseFragmentActivityExtensions>().forEach {
      if (it.onBackPressed(this) == BaseFragmentActivityExtensions.Super.ShouldCall) {
        super.onBackPressed()
      }
    }
  }

  abstract fun navigationDrawerIsOpen(): Boolean
  abstract fun closeNavigationDrawer()

  private fun requestOpenLink(
    alertDialogShower: AlertDialogShower,
    intent: Intent,
    sharedPreferenceUtil: SharedPreferenceUtil
  ) {
    alertDialogShower.show(
      KiwixDialog.ExternalLinkPopup, { ContextCompat.startActivity(this, intent, null) },
      {}, {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false)
        ContextCompat.startActivity(this, intent, null)
      })
  }

  private fun openExternalUrl(
    sharedPreferenceUtil: SharedPreferenceUtil,
    alertDialogShower: AlertDialogShower,
    intent: Intent
  ) {
    if (intent.resolveActivity(packageManager) != null) {
      // Show popup with warning that this url is external and could lead to additional costs
      // or may event not work when the user is offline.
      if (sharedPreferenceUtil.prefExternalLinkPopup) {
        requestOpenLink(alertDialogShower, intent, sharedPreferenceUtil)
      } else {
        openLink(intent)
      }
    } else {
      val error = getString(R.string.no_reader_application_installed)
      toast(error)
    }
  }

  private fun openLink(intent: Intent) {
    startActivity(intent)
  }

  private fun openSupportKiwixExternalLink() {
    openExternalUrl(
      sharedPreferenceUtil,
      alertDialogShower,
      KIWIX_SUPPORT_WEBSITE.toUri().browserIntent()
    )
  }

  abstract fun openSettingsActivity()

  private fun openHistoryActivity() {
    startActivityForResult(intent<HistoryActivity>(), REQUEST_HISTORY_ITEM_CHOSEN)
  }

  private fun openBookmarksActivity() {
    startActivityForResult(intent<BookmarksActivity>(), BOOKMARK_CHOSEN_REQUEST)
  }
}
