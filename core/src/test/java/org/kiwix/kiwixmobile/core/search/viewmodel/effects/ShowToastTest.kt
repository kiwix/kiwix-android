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

package org.kiwix.kiwixmobile.core.search.viewmodel.effects

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

internal class ShowToastTest {

  @Test
  fun `invoke with shows short toast`() {
    mockkStatic(Toast::class)
    val activity = mockk<AppCompatActivity>()
    val stringId = 0
    every { Toast.makeText(activity, stringId, Toast.LENGTH_SHORT).show() } just Runs
    ShowToast(stringId).invokeWith(activity)
  }
}
