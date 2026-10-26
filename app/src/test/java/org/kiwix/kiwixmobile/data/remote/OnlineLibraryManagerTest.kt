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

package org.kiwix.kiwixmobile.data.remote

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnlineLibraryManagerTest {
  private val onlineLibraryManager = OnlineLibraryManager(Dispatchers.Unconfined)

  @Test
  fun `buildLibraryUrl includes non-blank category`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      category = "wikipedia"
    )
    assertTrue(url.contains("category=wikipedia"))
  }

  @Test
  fun `buildLibraryUrl omits blank category so no category filter is sent`() {
    // Regression for #5211: an empty saved category must not add a category
    // parameter — the OPDS server treats any category value as a real filter
    // and returns zero entries.
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      query = "minecraft",
      lang = "eng",
      category = ""
    )
    assertFalse(url.contains("category="))
    assertTrue(url.contains("q=minecraft"))
    assertTrue(url.contains("lang=eng"))
  }

  @Test
  fun `buildLibraryUrl omits whitespace-only category`() {
    val url = onlineLibraryManager.buildLibraryUrl(
      "https://opds.library.kiwix.org",
      start = 0,
      count = 24,
      category = "   "
    )
    assertFalse(url.contains("category="))
  }
}
