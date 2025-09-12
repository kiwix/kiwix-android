/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

inline fun <reified T : ViewModel> Fragment.viewModel(
  viewModelFactory: ViewModelProvider.Factory
) = ViewModelProviders.of(this, viewModelFactory).get(T::class.java)

fun Fragment.toast(stringId: Int, length: Int = Toast.LENGTH_LONG) {
  requireActivity().toast(stringId, length)
}

fun Fragment.isKeyboardVisible(): Boolean {
  val insets = ViewCompat.getRootWindowInsets(requireView()) ?: return false
  return insets.isVisible(WindowInsetsCompat.Type.ime())
}

fun Fragment.closeKeyboard() {
  val inputMethodManager =
    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  inputMethodManager.hideSoftInputFromWindow(requireView().windowToken, 0)
}

fun View.closeKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(windowToken, 0)
}

val Fragment.coreMainActivity get() = activity as CoreMainActivity

/**
 * Updates the Activity window’s background color to match the current theme (light or dark).
 *
 * Since the status bar is drawn over the window background in edge-to-edge mode,
 * this effectively controls the visible status bar color and ensures proper
 * contrast with dark mode or light mode.
 */
fun Fragment.setStatusBarColor() {
  val darkModeActivity = CoreApp.instance.themeConfig.isDarkTheme()
  val windowBackGroundColor = if (darkModeActivity) {
    Color.BLACK
  } else {
    Color.WHITE
  }
  activity?.window?.decorView?.setBackgroundColor(windowBackGroundColor)
}
