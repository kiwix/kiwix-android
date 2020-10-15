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

package org.kiwix.kiwixmobile.core.main

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetUtilsKtTest {

  val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun readAssetAsText_withValidFile_assertContentReturns() {
    val fileContent = context.readAssetAsText("js/documentParser.js")
    assertTrue(fileContent.isNotEmpty())
  }

  @Test
  fun readAssetAsText_withFileNotFound_assertEmptyContentReturnsAndNoThrowException() {
    val fileContent = context.readAssetAsText("js/someFileThatWontBeFound.js")
    assertTrue(fileContent.isEmpty())
  }
}
