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

package org.kiwix.kiwixmobile.core.extensions

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.EXTRA_EXTERNAL_LINK
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

object ActivityExtensions {

  fun AppCompatActivity.startActionMode(
    menuId: Int,
    idsToClickActions: Map<Int, () -> Any>,
    onDestroyAction: () -> Unit
  ): ActionMode? {
    return startSupportActionMode(object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
        idsToClickActions[item.itemId]?.let {
          it()
          mode.finish()
          true
        } ?: false

      override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(menuId, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

      override fun onDestroyActionMode(mode: ActionMode?) {
        onDestroyAction()
      }
    })
  }

  fun Activity.openExternalUrl(
    sharedPreferenceUtil: SharedPreferenceUtil,
    alertDialogShower: AlertDialogShower,
    intent: Intent
  ) {
    if (intent.resolveActivity(packageManager) != null) {
      // Show popup with warning that this url is external and could lead to additional costs
      // or may event not work when the user is offline.
      if (urlIntentIsValid(intent) && sharedPreferenceUtil.prefExternalLinkPopup) {
        requestOpenLink(alertDialogShower, intent, sharedPreferenceUtil)
      } else {
        openLink(intent)
      }
    } else {
      val error = getString(R.string.no_reader_application_installed)
      Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }
  }

  private fun Activity.openLink(intent: Intent) {
    ContextCompat.startActivity(this, intent, null)
  }

  private fun urlIntentIsValid(intent: Intent) =
    (intent.hasExtra(EXTRA_EXTERNAL_LINK) && intent.getBooleanExtra(EXTRA_EXTERNAL_LINK, false))

  private fun Activity.requestOpenLink(
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

  inline fun <reified T : Activity> Activity.start(
    noinline intentFunc: (Intent.() -> Unit)? = null
  ) {
    startActivity(intent<T> { intentFunc?.invoke(this) })
  }

  inline fun <reified T : Activity> Activity.intent(
    noinline intentFunc: (Intent.() -> Unit)? = null
  ) =
    Intent(this, T::class.java).apply { intentFunc?.invoke(this) }

  inline fun <reified T : ViewModel> FragmentActivity.viewModel(
    viewModelFactory: ViewModelProvider.Factory
  ) =
    ViewModelProviders.of(this, viewModelFactory)
      .get(T::class.java)

  val Activity.coreActivityComponent
    get() = CoreApp.coreComponent.activityComponentBuilder().activity(this).build()
}
