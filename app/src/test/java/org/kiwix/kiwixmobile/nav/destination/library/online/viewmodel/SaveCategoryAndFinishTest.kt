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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import androidx.appcompat.app.AppCompatActivity
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category

class SaveCategoryAndFinishTest {
  @Test
  fun `invoke saves single category and finishes`() = runTest {
    val kiwixDataStore = mockk<KiwixDataStore>()
    val activity = mockk<AppCompatActivity>()
    val lifeCycleScope = TestScope(testScheduler)
    val onDismiss = mockk<() -> Unit>(relaxed = true)
    val category = Category(category = "wikipedia", active = true)
    SaveCategoryAndFinish(listOf(category), kiwixDataStore, lifeCycleScope, onDismiss).invokeWith(activity)
    testScheduler.advanceUntilIdle()
    coEvery { kiwixDataStore.setSelectedOnlineContentCategory(category.category) }
    testScheduler.advanceUntilIdle()
    verify { onDismiss() }
  }

  @Test
  fun `invoke saves multiple categories and finishes`() = runTest {
    val kiwixDataStore = mockk<KiwixDataStore>()
    val activity = mockk<AppCompatActivity>()
    val lifeCycleScope = TestScope(testScheduler)
    val onDismiss = mockk<() -> Unit>(relaxed = true)
    val category1 = Category(category = "wikipedia", active = true)
    val category2 = Category(category = "gutenberg", active = true)
    val categories = listOf(category1, category2)
    SaveCategoryAndFinish(categories, kiwixDataStore, lifeCycleScope, onDismiss).invokeWith(activity)
    testScheduler.advanceUntilIdle()
    coEvery { kiwixDataStore.setSelectedOnlineContentCategory("wikipedia,gutenberg") }
    testScheduler.advanceUntilIdle()
    verify { onDismiss() }
  }
}
