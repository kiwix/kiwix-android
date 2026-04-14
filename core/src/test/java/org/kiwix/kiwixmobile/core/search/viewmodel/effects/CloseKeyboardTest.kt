/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.search.viewmodel.effects

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.kiwix.kiwixmobile.core.utils.effects.CloseKeyboard

internal class CloseKeyboardTest {
  private val activity: AppCompatActivity = mockk()

  @Test
  fun invokeWith_whenCurrentFocusIsNull_DoesNoAction() {
    every { activity.currentFocus } returns null
    CloseKeyboard.invokeWith(activity)
    verify { activity.currentFocus }
    confirmVerified(activity)
  }

  @Test
  fun invokeWith_whenCurrentFocusIsNotNull_ClosesKeyboard() {
    val focusedView: View = mockk(relaxed = true)
    val imm: InputMethodManager = mockk(relaxed = true)
    val context: Context = mockk()

    every { activity.currentFocus } returns focusedView
    every { focusedView.context } returns context
    every {
      context.getSystemService(Context.INPUT_METHOD_SERVICE)
    } returns imm

    CloseKeyboard.invokeWith(activity)

    verify { activity.currentFocus }
    verify { context.getSystemService(Context.INPUT_METHOD_SERVICE) }
    verify { imm.hideSoftInputFromWindow(focusedView.windowToken, 0) }
  }
}
