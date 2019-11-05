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
import android.view.ActionMode
import android.view.ActionMode.Callback
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.kiwix.kiwixmobile.core.Intents

object ActivityExtensions {

  fun Activity.startActionMode(
    menuId: Int,
    idsToClickActions: Map<Int, () -> Any>,
    onDestroyAction: () -> Unit
  ): ActionMode? {
    return startActionMode(object : Callback {
      override fun onActionItemClicked(
        mode: ActionMode,
        item: MenuItem
      ) = idsToClickActions[item.itemId]?.let {
        it()
        mode.finish()
        true
      } ?: false

      override fun onCreateActionMode(
        mode: ActionMode,
        menu: Menu?
      ): Boolean {
        mode.menuInflater
          .inflate(menuId, menu)
        return true
      }

      override fun onPrepareActionMode(
        mode: ActionMode?,
        menu: Menu?
      ) = false

      override fun onDestroyActionMode(mode: ActionMode?) {
        onDestroyAction()
      }
    })
  }

  inline fun <reified T : Activity> Activity.start(
    noinline intentFunc: (Intent.() -> Unit)? = null
  ) {
    startActivity(
      Intent(this, T::class.java).apply {
        intentFunc?.invoke(this)
      }
    )
  }

  inline fun <reified T : Activity> Activity.startWithActionFrom() {
    startActivity(Intents.internal(T::class.java))
  }

  inline fun <reified T : ViewModel> FragmentActivity.viewModel(
    viewModelFactory: ViewModelProvider.Factory
  ) =
    ViewModelProviders.of(this, viewModelFactory)
      .get(T::class.java)
}
