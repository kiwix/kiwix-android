/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.history

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.DELETE_MENU_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import javax.inject.Inject

class NavigationHistoryDialog(
  @StringRes private val titleId: Int,
  private val navigationHistoryList: MutableList<NavigationHistoryListItem>,
  private val navigationHistoryClickListener: NavigationHistoryClickListener
) : DialogFragment() {
  private var composeView: ComposeView? = null

  @Inject
  lateinit var alertDialogShower: AlertDialogShower

  override fun onStart() {
    super.onStart()
    dialog?.let {
      it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    CoreApp.coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    return ComposeView(requireContext()).apply {
      setContent {
        NavigationHistoryDialogScreen(
          titleId,
          navigationHistoryList,
          listOf(
            ActionMenuItem(
              IconItem.Drawable(R.drawable.ic_delete_white_24dp),
              R.string.pref_clear_all_history_title,
              { showConfirmClearHistoryDialog() },
              isEnabled = navigationHistoryList.isNotEmpty(),
              testingTag = DELETE_MENU_ICON_TESTING_TAG
            )
          ),
          { onItemClick(it) },
          {
            NavigationIcon(
              iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
              onClick = {
                dismissNavigationHistoryDialog()
              }
            )
          }
        )
        DialogHost(alertDialogShower)
      }
    }.also {
      composeView = it
    }
  }

  private fun showConfirmClearHistoryDialog() {
    alertDialogShower.show(KiwixDialog.ClearAllNavigationHistory, ::clearHistory)
  }

  private fun clearHistory() {
    dismissNavigationHistoryDialog()
    navigationHistoryClickListener.clearHistory()
  }

  // Add onBackPressedCallBack to respond to user pressing 'Back' button on navigation bar
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Dialog(requireContext(), theme)
    requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallBack)
    return dialog
  }

  private val onBackPressedCallBack =
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        dismissNavigationHistoryDialog()
      }
    }

  private fun dismissNavigationHistoryDialog() {
    dialog?.dismiss()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    composeView?.disposeComposition()
    composeView = null
    onBackPressedCallBack.remove()
  }

  private fun onItemClick(item: NavigationHistoryListItem) {
    dismissNavigationHistoryDialog()
    navigationHistoryClickListener.onItemClicked(item)
  }

  companion object {
    const val TAG = "NavigationHistoryDialog"
  }
}
