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

package org.kiwix.kiwixmobile.workManager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.utils.workManager.UpdateWorkManager

@RunWith(AndroidJUnit4::class)
class UpdateWorkerTest {
  private var context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var workManager: UpdateWorkManager

  @Before
  fun setUp() {
    val app = context.applicationContext as CoreApp
    workManager = TestListenableWorkerBuilder<UpdateWorkManager>(context)
      .setWorkerFactory(app.updateWorkerFactory)
      .build()
  }

  @Test
  fun testUpdateWorker() {
    runBlocking {
      val result = workManager.doWork()
      assertThat(result, `is`(ListenableWorker.Result.success()))
    }
  }
}
