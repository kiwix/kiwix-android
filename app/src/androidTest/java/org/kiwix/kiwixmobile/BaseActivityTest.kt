/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Rule
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.di.components.DaggerTestComponent
import org.kiwix.kiwixmobile.core.di.components.TestComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest {
  @get:Rule
  open var activityScenarioRule = ActivityScenarioRule(KiwixMainActivity::class.java)

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }

  protected fun testComponent(): TestComponent = DaggerTestComponent.builder()
    .context(context)
    .build()

  abstract fun waitForIdle()
}
