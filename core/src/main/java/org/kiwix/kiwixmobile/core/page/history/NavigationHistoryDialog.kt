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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.databinding.DialogNavigationHistoryBinding
import org.kiwix.kiwixmobile.core.main.DISABLE_ICON_ITEM_ALPHA
import org.kiwix.kiwixmobile.core.main.ENABLE_ICON_ITEM_ALPHA
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryAdapter
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryDelegate.NavigationDelegate
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import javax.inject.Inject

class NavigationHistoryDialog(
  private val toolbarTitle: String,
  private val navigationHistoryList: MutableList<NavigationHistoryListItem>,
  private val navigationHistoryClickListener: NavigationHistoryClickListener
) : DialogFragment() {

  private var dialogNavigationHistoryBinding: DialogNavigationHistoryBinding? = null
  private var navigationHistoryAdapter: NavigationHistoryAdapter? = null

  @Inject
  lateinit var alertDialogShower: AlertDialogShower

  private val toolbar by lazy {
    dialogNavigationHistoryBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
  }
  private val deleteItem by lazy { toolbar?.menu?.findItem(R.id.menu_pages_clear) }

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
    dialogNavigationHistoryBinding =
      DialogNavigationHistoryBinding.inflate(inflater, container, false)
    return dialogNavigationHistoryBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    navigationHistoryAdapter = NavigationHistoryAdapter(NavigationDelegate(::onItemClick)).apply {
      items = navigationHistoryList
    }
    toolbar?.apply {
      title = toolbarTitle
      setNavigationIcon(R.drawable.ic_close_white_24dp)
      setNavigationOnClickListener { dismissNavigationHistoryDialog() }
      inflateMenu(R.menu.menu_page)
      this.menu?.findItem(R.id.menu_page_search)?.isVisible = false
    }
    dialogNavigationHistoryBinding?.apply {
      if (navigationHistoryList.isEmpty()) {
        deleteItem?.isEnabled = false
        deleteItem?.icon?.alpha = DISABLE_ICON_ITEM_ALPHA
        searchNoResults.visibility = View.VISIBLE
        navigationHistoryRecyclerView.visibility = View.GONE
      } else {
        deleteItem?.isEnabled = true
        deleteItem?.icon?.alpha = ENABLE_ICON_ITEM_ALPHA
        searchNoResults.visibility = View.GONE
        navigationHistoryRecyclerView.visibility = View.VISIBLE
      }
      navigationHistoryRecyclerView.run {
        adapter = navigationHistoryAdapter
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        setHasFixedSize(true)
      }
    }
    deleteItem?.setOnMenuItemClickListener {
      showConfirmClearHistoryDialog()
      true
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
    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner, onBackPressedCallBack
    )
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
    navigationHistoryAdapter = null
    dialogNavigationHistoryBinding = null
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
