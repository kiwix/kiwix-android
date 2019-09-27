package org.kiwix.kiwixmobile.extensions

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

inline fun <reified T : Activity> Activity.start() {
  startActivity(Intent(this, T::class.java))
}

inline fun <reified T : ViewModel> FragmentActivity.viewModel(
  viewModelFactory: ViewModelProvider.Factory
) =
  ViewModelProviders.of(this, viewModelFactory)
    .get(T::class.java)
