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

package org.kiwix.kiwixmobile.category.viewmodel

import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.SaveCategoryAndFinish

class SaveCategoryAndFinishTest {
  @Test
  fun `invoke saves category and finishes`() = runTest {
    val sharedPreferenceUtil = mockk<SharedPreferenceUtil>()
    val activity = mockk<AppCompatActivity>()
    val lifeCycleScope = TestScope(testScheduler)
    val onBackPressedDispatcher = mockk<OnBackPressedDispatcher>()
    every { activity.onBackPressedDispatcher } returns onBackPressedDispatcher
    every { onBackPressedDispatcher.onBackPressed() } answers { }
    val category = Category(category = "wikipedia", active = true)
    SaveCategoryAndFinish(category, sharedPreferenceUtil, lifeCycleScope).invokeWith(activity)
    testScheduler.advanceUntilIdle()
    every { sharedPreferenceUtil.selectedOnlineContentCategory == category.category }
    testScheduler.advanceUntilIdle()
    verify { onBackPressedDispatcher.onBackPressed() }
  }
}
