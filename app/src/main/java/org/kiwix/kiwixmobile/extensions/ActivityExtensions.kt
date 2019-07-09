package org.kiwix.kiwixmobile.extensions

import android.app.Activity
import android.view.ActionMode
import android.view.ActionMode.Callback
import android.view.Menu
import android.view.MenuItem

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
      mode.getMenuInflater()
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
