/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import javax.inject.Inject

const val ONLINE_CATEGORY_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG =
  "onlineCategoryDialogCloseImageButtonTestingTag"
const val ONLINE_CATEGORY_DIALOG_TAG = "onlineCategoryDialogTag"

class OnlineCategoryDialog : DialogFragment() {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val categoryViewModel by lazy { viewModel<CategoryViewModel>(viewModelFactory) }

  // Add onBackPressedCallBack to respond to user pressing 'Back' button on navigation bar
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Dialog(requireContext())
    requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallBack)
    return dialog
  }

  private val onBackPressedCallBack =
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        dismissDialog()
      }
    }

  private fun dismissDialog() {
    dialog?.dismiss()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (activity as? BaseActivity)?.cachedComponent?.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).apply {
    setContent {
      OnlineCategoryDialogScreen(
        categoryViewModel = categoryViewModel,
        navigationIcon = {
          NavigationIcon(
            iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
            onClick = {
              dismissDialog()
            },
            testingTag = ONLINE_CATEGORY_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG
          )
        }
      )
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.let {
      it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    onBackPressedCallBack.remove()
  }
}
